import React from "react";
import { Bell } from "lucide-react";
import UserProfile from "./Userprofile";

export default function Topbar({ user, handleLogout }) {
  return (
    <header className="bg-[#31694E] text-white px-40 py-3 flex items-center justify-between shadow-md">
      {/* Logo */}
      <div className="font-bold text-lg tracking-wider cursor-pointer">
        MADA-TRANSIT
      </div>

      {/* Espace central */}
      <div className="flex-1" />

      {/* Actions */}
      <div className="flex items-center space-x-6">
        <button className="relative hover:text-gray-300 transition p-1">
          <Bell size={22} />

          <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
        </button>

        <UserProfile user={user} handleLogout={handleLogout} />
      </div>
    </header>
  );
}
