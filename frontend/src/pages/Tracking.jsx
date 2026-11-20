import React, { useState, useEffect } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
} from "react-leaflet";
import { Navigation, MapPin, Bus, ShieldAlert } from "lucide-react";
import L from "leaflet";

import icon from "leaflet/dist/images/marker-icon.png";
import iconShadow from "leaflet/dist/images/marker-shadow.png";
import api from "../hooks/api";
let DefaultIcon = L.icon({
  iconUrl: icon,
  shadowUrl: iconShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});
L.Marker.prototype.options.icon = DefaultIcon;

const busIcon = new L.Icon({
  iconUrl: "https://cdn-icons-png.flaticon.com/512/3448/3448339.png",
  iconSize: [32, 32],
  iconAnchor: [16, 32],
  popupAnchor: [0, -32],
});

export default function TrackingContent() {
  const fianaCenter = [-21.4526, 47.0857];

  const [lines, setLines] = useState([]);

  const [stops] = useState([
    {
      id_stop: 1,
      name: "Terminus Andrainjato",
      coords: [-21.4585, 47.1002],
      line: "L1",
    },
    {
      id_stop: 2,
      name: "Antanifotsy",
      coords: [-21.4552, 47.0918],
      line: "L1",
    },
    {
      id_stop: 3,
      name: "Stationnement",
      coords: [-21.4491, 47.0811],
      line: "L1",
    },
    { id_stop: 4, name: "Anjoma", coords: [-21.4422, 47.0722], line: "L1" },
    { id_stop: 5, name: "Faritany", coords: [-21.4468, 47.0761], line: "L1" },
    {
      id_stop: 6,
      name: "Arrêt Mahazengy",
      coords: [-21.4532, 47.0868],
      line: "L1",
    },

    {
      id_stop: 7,
      name: "Station Tambohobe",
      coords: [-21.4395, 47.065],
      line: "L2",
    },
    {
      id_stop: 8,
      name: "Arrêt Beravina",
      coords: [-21.462, 47.0795],
      line: "L2",
    },
  ]);

  const [buses] = useState([
    {
      id_bus: 101,
      license_plate: "5421 TAB",
      line_id: "L1",
      driver: "Jean Rakoto",
      current_coords: [-21.4491, 47.0811],
      status: "En Route",
    },
    {
      id_bus: 204,
      license_plate: "8974 TAA",
      line_id: "L2",
      driver: "Rindra Randria",
      current_coords: [-21.445, 47.071],
      status: "En Route",
    },
  ]);

  const [selectedLine, setSelectedLine] = useState("");

  const fetchBuses = async () => {
    try {
      const response = await api.get("/buses");
      const res = response.data.data;
      console.log("Réponse de l'API :", res);
      setBuses(res);
    } catch (error) {
      console.error("Erreur lors de la récupération des bus :", error);
    }
  };

  const fetchStops = async () => {
    try {
      const response = await api.get("/stops");
      const res = response.data.data;
      console.log("Réponse de l'API :", res);
      setStops(res);
    } catch (error) {
      console.error("Erreur lors de la récupération des arrêts :", error);
    }
  };

  const fetchLines = async () => {
    const response = await api.get("/lines");

    const formattedLines = response.data.data.map((line) => ({
      id_line: line.id_line,
      name_line: line.line_name,
      description: line.description,
      price: line.price,

      color: "#3B3B98",

      route_path: line.road_path,

      stops: line.lineStops.map((item) => ({
        id_stop: item.stop.id_stop,
        name: item.stop.name_stop,
        coords: [Number(item.stop.latitude), Number(item.stop.longitude)],
      })),

      bus: line.bus,
    }));

    setLines(formattedLines);
  };

  useEffect(() => {
    fetchLines();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
        <div>
          <h2 className="text-sm font-bold text-gray-800 uppercase tracking-wide flex items-center">
            <Navigation
              size={18}
              className="text-[#3B3B98] mr-2 animate-pulse"
            />
            Cartographie & Suivi du Réseau
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            Visualisation des itinéraires, arrêts et positions des véhicules
          </p>
        </div>

        <div className="w-full sm:w-64">
          <select
            value={selectedLine}
            onChange={(e) => setSelectedLine(e.target.value)}
            className="w-full bg-gray-50 border border-gray-200 px-3 py-2 rounded-lg text-sm outline-none focus:border-[#3B3B98] focus:bg-white font-medium transition-all"
          >
            <option value="">Afficher tout le réseau</option>
            {lines.map((line) => (
              <option key={line.id_line} value={line.id_line}>
                {line.name_line}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3 bg-white rounded-2xl shadow-sm p-2 border border-gray-100 overflow-hidden h-175 z-10">
          <MapContainer
            center={fianaCenter}
            zoom={14}
            style={{ height: "100%", width: "100%", borderRadius: "0.75rem" }}
          >
            <TileLayer
              url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png?key=cb1_2i9j_1_59049f13600fe6074ed624c1"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
            />

            {lines
              .filter(
                (line) =>
                  selectedLine === "" || line.id_line === Number(selectedLine),
              )
              .map((line) => (
                <Polyline
                  key={line.id_line}
                  positions={line.route_path}
                  pathOptions={{
                    color: line.color,
                    weight: 5,
                    opacity: 0.85,
                    lineCap: "round",
                    lineJoin: "round",
                  }}
                />
              ))}

            {lines.map((line) =>
              line.stops.map((stop) => (
                <Marker key={stop.id_stop} position={stop.coords}>
                  <Popup>{stop.name}</Popup>
                </Marker>
              )),
            )}

            {/* {lines
              .map((bus) => (
                <Marker
                  key={bus.bus.id_bus}
                  position={bus.current_coords}
                  icon={busIcon}
                >
                  <Popup>
                    <div className="text-xs space-y-1.5 p-0.5">
                      <div className="font-bold text-gray-800 flex items-center bg-gray-50 p-1 rounded">
                        <Bus size={14} className="text-[#3B3B98] mr-1" />
                        Bus N° {bus.id_bus}
                      </div>
                      <div className="text-[11px] font-medium">
                        Matricule :{" "}
                        <span className="font-mono bg-amber-100 px-1 rounded text-xs font-bold">
                          {bus.license_plate}
                        </span>
                      </div>
                      <div className="text-[11px]">
                        Chauffeur : <b>{bus.driver}</b>
                      </div>
                      <div className="text-[10px] text-green-600 font-bold">
                        ● Statut : {bus.status}
                      </div>
                    </div>
                  </Popup>
                </Marker>
              ))} */}
          </MapContainer>
        </div>

        <div className="space-y-4">
          <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 space-y-3">
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider font-mono">
              Légende des Lignes
            </h3>
            <div className="space-y-2">
              {lines.map((line) => (
                <button
                  key={line.id_line}
                  onClick={() => setSelectedLine(line.id_line)}
                  className={`w-full text-left p-2 rounded-lg text-xs flex items-center space-x-3 transition-colors ${selectedLine === line.id_line ? "bg-gray-50 font-bold border border-gray-200" : "hover:bg-gray-50"}`}
                >
                  <span
                    className="w-3 h-3 rounded-full shrink-0"
                    style={{ backgroundColor: line.color }}
                  ></span>
                  <span className="truncate text-gray-700">
                    {line.line_name}
                  </span>
                </button>
              ))}
            </div>
          </div>

          <div className="bg-amber-50 border border-amber-200 p-4 rounded-xl space-y-2">
            <div className="flex items-center text-amber-800 font-bold text-xs uppercase tracking-wider font-mono">
              <ShieldAlert size={16} className="mr-1.5 shrink-0" />
              Mode Simulation Actif
            </div>
            <p className="text-[11px] text-amber-700 leading-relaxed">
              Le suivi routier dynamique utilise l'API publique OSRM pour
              adapter les itinéraires physiques à la voirie urbaine de
              Fianarantsoa.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
