import React from "react";
import {
  LayoutDashboard,
  Bus,
  UserSquare2,
  Ticket,
  Users,
  MapPin,
} from "lucide-react";

import NavItem from "./NavItem";

export default function Navbar() {
  const menus = [
    {
      id: "dashboard",
      name: "Dashboard",
      path: "/dashboard",
      icon: <LayoutDashboard size={20} />,
    },
    {
      id: "buses",
      name: "Bus",
      path: "/buses",
      icon: <Bus size={20} />,
    },
    {
      id: "driver",
      name: "Chauffeur",
      path: "/drivers",
      icon: <UserSquare2 size={20} />,
    },
    {
      id: "tickets",
      name: "Tickets",
      path: "/tickets",
      icon: <Ticket size={20} />,
    },
    {
      id: "users",
      name: "Utilisateurs",
      path: "/users",
      icon: <Users size={20} />,
    },
    {
      id: "tracking",
      name: "Tracking",
      path: "/tracking",
      icon: <MapPin size={20} />,
    },
  ];

  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm px-40">
      <div className=" mx-auto flex space-x-8">
        {menus.map((menu) => (
          <NavItem
            key={menu.id}
            name={menu.name}
            path={menu.path}
            icon={menu.icon}
          />
        ))}
      </div>
    </nav>
  );
}
