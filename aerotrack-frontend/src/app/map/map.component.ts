import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import * as L from 'leaflet';

// ---------- API types (match backend Dtos.java) ----------
interface Airport { icao: string; iata: string | null; name: string; city: string; country: string; lat: number; lng: number; }
interface Flight {
  hex: string; callsign: string | null; registration: string | null; type: string | null;
  lat: number; lng: number; altitude: number | null; onGround: boolean; speed: number | null; track: number | null;
}
interface FlightDetails extends Flight {
  aircraftName: string | null;
  airline: { icao: string; name: string | null } | null;
  route: { origin: Airport; destination: Airport } | null;
  verticalRate: number | null; squawk: string | null; lastSeen: number;
}
interface TrailPoint { lat: number; lng: number; altitude: number | null; t: number; }
interface SearchResult { kind: 'flight' | 'airport'; label: string; sub: string; flight?: Flight; airport?: Airport; }

type ConnectionState = 'connecting' | 'online' | 'offline';

const PLANE_PATH = 'M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z';
const KARNATAKA_CENTER: L.LatLngExpression = [14.5, 76.2];

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapEl', { static: true }) mapEl!: ElementRef<HTMLDivElement>;

  private map!: L.Map;
  private markers = new Map<string, L.Marker>();
  private airportLayer = L.layerGroup();
  private trailLayer = L.layerGroup();
  private pollTimer: any;
  private detailsTimer: any;

  // ---- UI state ----
  flights = signal<Flight[]>([]);
  airports = signal<Airport[]>([]);
  selected = signal<FlightDetails | null>(null);
  selectedHex = signal<string | null>(null);
  connection = signal<ConnectionState>('connecting');
  lastUpdated = signal<Date | null>(null);
  query = signal('');
  searchFocused = signal(false);
  showAirports = signal(true);

  results = computed<SearchResult[]>(() => {
    const q = this.query().trim().toUpperCase();
    if (q.length < 2) return [];
    const flights: SearchResult[] = this.flights()
      .filter(f => (f.callsign ?? '').toUpperCase().includes(q) || (f.registration ?? '').toUpperCase().includes(q) || f.hex.toUpperCase().includes(q))
      .slice(0, 8)
      .map(f => ({ kind: 'flight', label: f.callsign || f.registration || f.hex.toUpperCase(), sub: [f.registration, f.type].filter(Boolean).join(' · '), flight: f }));
    const airports: SearchResult[] = this.airports()
      .filter(a => a.icao.includes(q) || (a.iata ?? '').includes(q) || a.name.toUpperCase().includes(q) || a.city.toUpperCase().includes(q))
      .slice(0, 5)
      .map(a => ({ kind: 'airport', label: `${a.iata ?? a.icao} · ${a.city}`, sub: a.name, airport: a }));
    return [...flights, ...airports];
  });

  constructor(private http: HttpClient) {}

  ngAfterViewInit(): void {
    this.initMap();
    this.loadAirports();
    this.fetchFlights();
    // Backend refreshes from ADS-B every 5 s; we read our own DB every 3 s
    this.pollTimer = setInterval(() => this.fetchFlights(), 3000);
  }

  ngOnDestroy(): void {
    clearInterval(this.pollTimer);
    clearInterval(this.detailsTimer);
    this.map?.remove();
  }

  // ---------------- Map setup ----------------

  private initMap(): void {
    this.map = L.map(this.mapEl.nativeElement, { center: KARNATAKA_CENTER, zoom: 7, zoomControl: false });
    L.control.zoom({ position: 'bottomright' }).addTo(this.map);

    // Base maps that need NO API key
    const dark = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 16, attribution: 'Tiles © Esri · Data © adsb.lol (ODbL)' });
    const street = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      { maxZoom: 18, attribution: '© OpenStreetMap contributors · Data © adsb.lol (ODbL)' });
    const satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 18, attribution: 'Tiles © Esri · Data © adsb.lol (ODbL)' });

    dark.addTo(this.map);
    L.control.layers({ Dark: dark, Street: street, Satellite: satellite }, undefined, { position: 'topright' }).addTo(this.map);

    // Fallback to OSM if dark tiles fail
    let tileErrors = 0;
    dark.on('tileerror', () => {
      if (++tileErrors === 3 && this.map.hasLayer(dark)) { this.map.removeLayer(dark); street.addTo(this.map); }
    });

    this.trailLayer.addTo(this.map);
    this.airportLayer.addTo(this.map);

    // Module 2: re-run the PostGIS viewport query when the user stops panning/zooming
    this.map.on('moveend', () => this.fetchFlights());
    this.map.on('click', () => this.clearSelection());

    setTimeout(() => this.map.invalidateSize(), 0);
  }

  // ---------------- Data ----------------

  private loadAirports(): void {
    this.http.get<Airport[]>('/api/airports').subscribe(airports => {
      this.airports.set(airports);
      this.renderAirports();
    });
  }

  private fetchFlights(): void {
    if (!this.map) return;
    const b = this.map.getBounds();
    const url = `/api/flights?minLat=${b.getSouth()}&minLng=${b.getWest()}&maxLat=${b.getNorth()}&maxLng=${b.getEast()}`;

    this.http.get<Flight[]>(url).subscribe({
      next: flights => {
        this.connection.set('online');
        this.lastUpdated.set(new Date());
        this.flights.set(flights);
        this.renderFlights(flights);
      },
      error: err => {
        console.error('Failed to fetch flights:', err);
        this.connection.set('offline');
      }
    });
  }

  private loadDetails(hex: string): void {
    this.http.get<FlightDetails>(`/api/flights/${hex}`).subscribe({
      next: d => {
        if (this.selectedHex() !== hex) return;
        this.selected.set(d);
        this.loadTrail(hex, d);
      },
      error: () => { if (this.selectedHex() === hex) this.clearSelection(); } // aircraft left coverage
    });
  }

  private loadTrail(hex: string, d: FlightDetails): void {
    this.http.get<TrailPoint[]>(`/api/flights/${hex}/trail`).subscribe(points => {
      if (this.selectedHex() !== hex) return;
      this.trailLayer.clearLayers();

      // Dashed great-circle-ish line: origin -> plane -> destination (like FR24)
      if (d.route) {
        const style = { color: '#9ca3af', weight: 1.5, opacity: 0.7, dashArray: '4 6' };
        L.polyline([[d.route.origin.lat, d.route.origin.lng], [d.lat, d.lng]], style).addTo(this.trailLayer);
        L.polyline([[d.lat, d.lng], [d.route.destination.lat, d.route.destination.lng]], style).addTo(this.trailLayer);
        for (const a of [d.route.origin, d.route.destination]) {
          L.circleMarker([a.lat, a.lng], { radius: 5, color: '#fff', weight: 2, fillColor: '#111827', fillOpacity: 1 })
            .bindTooltip(a.iata ?? a.icao, { permanent: true, direction: 'top', className: 'airport-label' })
            .addTo(this.trailLayer);
        }
      }

      // Real flown path, coloured by altitude segment
      const path = [...points, { lat: d.lat, lng: d.lng, altitude: d.altitude, t: Date.now() }];
      for (let i = 1; i < path.length; i++) {
        L.polyline([[path[i - 1].lat, path[i - 1].lng], [path[i].lat, path[i].lng]],
          { color: this.altitudeColor(path[i].altitude), weight: 3, opacity: 0.95 }).addTo(this.trailLayer);
      }
    });
  }

  // ---------------- Rendering ----------------

  private renderFlights(flights: Flight[]): void {
    const seen = new Set<string>();
    for (const f of flights) {
      seen.add(f.hex);
      const icon = this.planeIcon(f, f.hex === this.selectedHex());
      let m = this.markers.get(f.hex);
      if (m) {
        m.setLatLng([f.lat, f.lng]).setIcon(icon);
      } else {
        m = L.marker([f.lat, f.lng], { icon })
          .on('click', e => { L.DomEvent.stopPropagation(e); this.select(f.hex); })
          .addTo(this.map);
        this.markers.set(f.hex, m);
      }
      m.unbindTooltip().bindTooltip(f.callsign || f.registration || f.hex.toUpperCase(),
        { direction: 'right', offset: [10, 0], className: 'plane-tooltip' });
    }
    for (const [hex, m] of this.markers) {
      if (!seen.has(hex)) { this.map.removeLayer(m); this.markers.delete(hex); }
    }
  }

  private renderAirports(): void {
    this.airportLayer.clearLayers();
    if (!this.showAirports()) return;
    for (const a of this.airports()) {
      L.marker([a.lat, a.lng], {
        icon: L.divIcon({ className: 'airport-icon', html: `<span>${a.iata ?? a.icao}</span>`, iconSize: [36, 18], iconAnchor: [18, 9] }),
        zIndexOffset: -1000
      })
        .bindPopup(`<b>${a.name}</b><br>${a.city} · ${a.iata ?? '—'} / ${a.icao}`)
        .addTo(this.airportLayer);
    }
  }

  private planeIcon(f: Flight, selected: boolean): L.DivIcon {
    const color = selected ? '#ef4444' : f.onGround ? '#9ca3af' : '#facc15'; // FR24: yellow, red when selected
    const size = selected ? 30 : 24;
    return L.divIcon({
      html: `<svg viewBox="0 0 24 24" width="${size}" height="${size}" style="transform: rotate(${f.track ?? 0}deg)">
               <path fill="${color}" stroke="#1f2937" stroke-width="0.8" d="${PLANE_PATH}"/></svg>`,
      className: 'plane-icon',
      iconSize: [size, size],
      iconAnchor: [size / 2, size / 2]
    });
  }

  /** FR24-style altitude colour ramp (feet). */
  altitudeColor(alt: number | null): string {
    if (alt == null || alt <= 0) return '#9ca3af';
    if (alt < 2000) return '#f97316';
    if (alt < 10000) return '#facc15';
    if (alt < 20000) return '#4ade80';
    if (alt < 30000) return '#22d3ee';
    if (alt < 38000) return '#818cf8';
    return '#e879f9';
  }

  // ---------------- UI actions ----------------

  select(hex: string): void {
    this.selectedHex.set(hex);
    this.selected.set(null);
    this.trailLayer.clearLayers();
    this.renderFlights(this.flights());
    this.loadDetails(hex);
    clearInterval(this.detailsTimer);
    this.detailsTimer = setInterval(() => this.loadDetails(hex), 5000);
  }

  clearSelection(): void {
    clearInterval(this.detailsTimer);
    this.selectedHex.set(null);
    this.selected.set(null);
    this.trailLayer.clearLayers();
    this.renderFlights(this.flights());
  }

  pickResult(r: SearchResult): void {
    this.query.set('');
    this.searchFocused.set(false);
    if (r.flight) {
      this.map.flyTo([r.flight.lat, r.flight.lng], Math.max(this.map.getZoom(), 9), { duration: 0.8 });
      this.select(r.flight.hex);
    } else if (r.airport) {
      this.map.flyTo([r.airport.lat, r.airport.lng], 11, { duration: 0.8 });
    }
  }

  onSearchBlur(): void {
    setTimeout(() => this.searchFocused.set(false), 150); // allow click on result
  }

  toggleAirports(): void {
    this.showAirports.set(!this.showAirports());
    this.renderAirports();
  }

  recenter(): void {
    this.map.flyTo(KARNATAKA_CENTER, 7, { duration: 0.8 });
  }

  // ---------------- Formatting helpers ----------------

  progress(d: FlightDetails): number | null {
    if (!d.route) return null;
    const done = this.distanceKm(d.route.origin.lat, d.route.origin.lng, d.lat, d.lng);
    const left = this.distanceKm(d.lat, d.lng, d.route.destination.lat, d.route.destination.lng);
    return Math.round((done / (done + left)) * 100);
  }

  distanceToGo(d: FlightDetails): number | null {
    return d.route ? Math.round(this.distanceKm(d.lat, d.lng, d.route.destination.lat, d.route.destination.lng)) : null;
  }

  /** Haversine great-circle distance. */
  private distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const R = 6371, toRad = (x: number) => (x * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1), dLng = toRad(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(a));
  }

  airborneCount = computed(() => this.flights().filter(f => !f.onGround).length);
}
