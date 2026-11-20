import { Outlet } from "react-router-dom";

import Topbar from "../Topbar";
import Navbar from "../Navbar";

export default function DashboardLayout({ user, handleLogout }) {
  return (
    <div className="min-h-screen bg-[#D9D9D9]">
      <div className="flex flex-col fixed w-full z-50">
        <Topbar user={user} handleLogout={handleLogout} />

        <Navbar />
      </div>

      <main className="max-w-9xl mx-auto px-40 py-35">
        <Outlet />
      </main>
    </div>
  );
}
