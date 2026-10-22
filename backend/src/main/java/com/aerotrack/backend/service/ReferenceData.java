package com.aerotrack.backend.service;

import com.aerotrack.backend.service.Dtos.Airline;
import com.aerotrack.backend.service.Dtos.Airport;

import java.util.List;
import java.util.Map;

/**
 * Static reference data (no API calls needed): airports in Karnataka, common airlines seen over
 * South India (by ICAO callsign prefix), and human-readable names for ICAO aircraft type codes.
 */
public final class ReferenceData {

    private ReferenceData() {}

    public static final List<Airport> KARNATAKA_AIRPORTS = List.of(
            new Airport("VOBL", "BLR", "Kempegowda International Airport", "Bengaluru", "IN", 13.1986, 77.7066),
            new Airport("VOBG", null, "HAL Airport", "Bengaluru", "IN", 12.9500, 77.6682),
            new Airport("VOML", "IXE", "Mangaluru International Airport", "Mangaluru", "IN", 12.9613, 74.8901),
            new Airport("VOHB", "HBX", "Hubballi Airport", "Hubballi", "IN", 15.3617, 75.0849),
            new Airport("VOBM", "IXG", "Belagavi Airport", "Belagavi", "IN", 15.8593, 74.6183),
            new Airport("VOMY", "MYQ", "Mysuru Airport", "Mysuru", "IN", 12.2300, 76.6558),
            new Airport("VOGB", "GBI", "Kalaburagi Airport", "Kalaburagi", "IN", 17.3070, 76.9580),
            new Airport("VOBR", "IXX", "Bidar Airport", "Bidar", "IN", 17.9081, 77.4871),
            new Airport("VOSH", "RQY", "Shivamogga Airport", "Shivamogga", "IN", 13.8550, 75.6100),
            new Airport("VOJV", "VDY", "Jindal Vijaynagar Airport", "Ballari", "IN", 15.1750, 76.6340)
    );

    private static final Map<String, String> AIRLINES = Map.ofEntries(
            Map.entry("IGO", "IndiGo"),
            Map.entry("AIC", "Air India"),
            Map.entry("AXB", "Air India Express"),
            Map.entry("IAD", "Air India Express"),
            Map.entry("VTI", "Vistara"),
            Map.entry("AKJ", "Akasa Air"),
            Map.entry("SEJ", "SpiceJet"),
            Map.entry("LLR", "Alliance Air"),
            Map.entry("FLG", "Fly91"),
            Map.entry("SDG", "Star Air"),
            Map.entry("BDA", "Blue Dart Aviation"),
            Map.entry("ETD", "Etihad Airways"),
            Map.entry("UAE", "Emirates"),
            Map.entry("QTR", "Qatar Airways"),
            Map.entry("FDB", "flydubai"),
            Map.entry("ABY", "Air Arabia"),
            Map.entry("OMA", "Oman Air"),
            Map.entry("GFA", "Gulf Air"),
            Map.entry("KAC", "Kuwait Airways"),
            Map.entry("SVA", "Saudia"),
            Map.entry("JZR", "Jazeera Airways"),
            Map.entry("SIA", "Singapore Airlines"),
            Map.entry("TGW", "Scoot"),
            Map.entry("MAS", "Malaysia Airlines"),
            Map.entry("AXM", "AirAsia"),
            Map.entry("XAX", "AirAsia X"),
            Map.entry("THA", "Thai Airways"),
            Map.entry("ALK", "SriLankan Airlines"),
            Map.entry("CPA", "Cathay Pacific"),
            Map.entry("BAW", "British Airways"),
            Map.entry("VIR", "Virgin Atlantic"),
            Map.entry("DLH", "Lufthansa"),
            Map.entry("AFR", "Air France"),
            Map.entry("KLM", "KLM"),
            Map.entry("THY", "Turkish Airlines"),
            Map.entry("FIN", "Finnair"),
            Map.entry("ETH", "Ethiopian Airlines"),
            Map.entry("KQA", "Kenya Airways"),
            Map.entry("MSR", "EgyptAir"),
            Map.entry("QFA", "Qantas"),
            Map.entry("UAL", "United Airlines"),
            Map.entry("AAL", "American Airlines"),
            Map.entry("FDX", "FedEx"),
            Map.entry("UPS", "UPS Airlines"),
            Map.entry("BBC", "Biman Bangladesh"),
            Map.entry("RNA", "Nepal Airlines"),
            Map.entry("MDV", "Maldivian")
    );

    private static final Map<String, String> AIRCRAFT_TYPES = Map.ofEntries(
            Map.entry("A319", "Airbus A319"),
            Map.entry("A320", "Airbus A320"),
            Map.entry("A20N", "Airbus A320neo"),
            Map.entry("A321", "Airbus A321"),
            Map.entry("A21N", "Airbus A321neo"),
            Map.entry("A332", "Airbus A330-200"),
            Map.entry("A333", "Airbus A330-300"),
            Map.entry("A339", "Airbus A330-900neo"),
            Map.entry("A359", "Airbus A350-900"),
            Map.entry("A35K", "Airbus A350-1000"),
            Map.entry("A388", "Airbus A380-800"),
            Map.entry("BCS3", "Airbus A220-300"),
            Map.entry("B737", "Boeing 737-700"),
            Map.entry("B738", "Boeing 737-800"),
            Map.entry("B739", "Boeing 737-900"),
            Map.entry("B38M", "Boeing 737 MAX 8"),
            Map.entry("B39M", "Boeing 737 MAX 9"),
            Map.entry("B744", "Boeing 747-400"),
            Map.entry("B748", "Boeing 747-8"),
            Map.entry("B752", "Boeing 757-200"),
            Map.entry("B763", "Boeing 767-300"),
            Map.entry("B772", "Boeing 777-200"),
            Map.entry("B77L", "Boeing 777-200LR"),
            Map.entry("B77W", "Boeing 777-300ER"),
            Map.entry("B788", "Boeing 787-8 Dreamliner"),
            Map.entry("B789", "Boeing 787-9 Dreamliner"),
            Map.entry("B78X", "Boeing 787-10 Dreamliner"),
            Map.entry("AT72", "ATR 72"),
            Map.entry("AT75", "ATR 72-500"),
            Map.entry("AT76", "ATR 72-600"),
            Map.entry("DH8D", "De Havilland Dash 8-400"),
            Map.entry("E190", "Embraer E190"),
            Map.entry("E195", "Embraer E195"),
            Map.entry("E75L", "Embraer E175"),
            Map.entry("CRJ7", "Bombardier CRJ700"),
            Map.entry("CRJ9", "Bombardier CRJ900"),
            Map.entry("C208", "Cessna 208 Caravan"),
            Map.entry("C172", "Cessna 172"),
            Map.entry("PC12", "Pilatus PC-12"),
            Map.entry("GLF6", "Gulfstream G650"),
            Map.entry("GLEX", "Bombardier Global Express"),
            Map.entry("C17", "Boeing C-17 Globemaster III"),
            Map.entry("C130", "Lockheed C-130 Hercules")
    );

    /** Airline from callsign prefix, e.g. "IGO615C" -> IndiGo. */
    public static Airline airlineFor(String callsign) {
        if (callsign == null || callsign.length() < 4) return null;
        String prefix = callsign.substring(0, 3).toUpperCase();
        if (!prefix.chars().allMatch(Character::isLetter)) return null;
        String name = AIRLINES.get(prefix);
        return name == null ? new Airline(prefix, null) : new Airline(prefix, name);
    }

    public static String aircraftName(String typeCode) {
        if (typeCode == null) return null;
        return AIRCRAFT_TYPES.getOrDefault(typeCode.toUpperCase(), typeCode);
    }
}

