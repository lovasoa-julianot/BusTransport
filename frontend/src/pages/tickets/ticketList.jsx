import { useEffect, useRef, useState } from "react";
import {
  Edit2,
  Filter,
  ImageIcon,
  Plus,
  Printer,
  QrCode,
  Search,
  Trash2,
  UserPlus,
} from "lucide-react";
import Table from "../../components/common/table";
import Pagination from "../../components/common/pagination";
import axios from "axios";
import TicketForm from "./ticketForm";
import api from "../../hooks/api";
import Swal from "sweetalert2";
import QRCode from "react-qr-code";
import TicketCard from "../../components/common/ticketcard";
import { useReactToPrint } from "react-to-print";

export default function TicketList() {
  const [isEdit, setIsEdit] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [tickets, setTickets] = useState([]);

  const [stops, setStops] = useState([]);

  const [selectedTickets, setSelectedTickets] = useState([]);

  const printRef = useRef();

  const [selectedQR, setSelectedQR] = useState(null);

  const handlePrint = useReactToPrint({
    contentRef: printRef,
  });

  const getSops = async () => {
    try {
      const response = await api.get("/stops");
      setStops(response.data.data);
    } catch (error) {
      console.error("Erreur lors de la récupération des arrêts :", error);
    }
  };

  const handleSelectTicket = (id) => {
    setSelectedTickets((prev) =>
      prev.includes(id)
        ? prev.filter((ticketId) => ticketId !== id)
        : [...prev, id],
    );
  };

  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelectedTickets((prev) =>
        prev.filter(
          (id) => !filteredTickets.some((ticket) => ticket.id_ticket === id),
        ),
      );
    } else {
      setSelectedTickets((prev) => [
        ...new Set([
          ...prev,
          ...filteredTickets.map((ticket) => ticket.id_ticket),
        ]),
      ]);
    }
  };

  const handleDeleteSelected = async () => {
    if (selectedTickets.length === 0) return;

    const result = await Swal.fire({
      icon: "warning",
      title: "Supprimer les tickets ?",
      text: `Vous êtes sur le point de supprimer ${selectedTickets.length} ticket(s).`,
      showCancelButton: true,
      confirmButtonText: "Oui, supprimer",
      cancelButtonText: "Annuler",
      confirmButtonColor: "#d33",
    });

    if (!result.isConfirmed) return;

    try {
      await api.delete("/tickets", {
        data: {
          ids: selectedTickets,
        },
      });

      Swal.fire({
        icon: "success",
        title: "Supprimé",
        text: `${selectedTickets.length} ticket(s) supprimé(s) avec succès.`,
        timer: 2000,
        showConfirmButton: false,
      });

      setSelectedTickets([]);
      getAllTickets(currentPage);
    } catch (error) {
      console.error("Erreur lors de la suppression :", error);

      Swal.fire({
        icon: "error",
        title: "Erreur",
        text:
          error.response?.data?.error ||
          "Impossible de supprimer les tickets sélectionnés.",
      });
    }
  };

  const getAllTickets = async (page = 1) => {
    try {
      const response = await api.get(`/tickets?page=${page}&limit=10`);
      if (!response.data || !response.data.data) {
        console.error("Réponse de l'API invalide :", response.data);
        return;
      }
      console.log("Réponse de l'API :", response.data);
      setTickets(response.data.data);
      setCurrentPage(response.data.page);
      setTotalPages(response.data.totalPages);
    } catch (error) {
      console.error("Erreur lors de la récupération des tickets :", error);
    }
  };

  useEffect(() => {
    getSops();
  }, []);

  useEffect(() => {
    getAllTickets(currentPage);
  }, [currentPage]);

  const [ticketForm, setTicketForm] = useState({
    id_stop: 0,
    quantity: 0,
    price: 0,
  });

  const handleGenerateTickets = (e) => {
    e.preventDefault();

    api
      .post("/tickets/generate", {
        terminus_id: parseInt(ticketForm.id_stop),
        count: parseInt(ticketForm.quantity),
        price: parseFloat(ticketForm.price),
      })
      .then((response) => {
        if (response.data.success) {
          Swal.fire({
            icon: "success",
            title: "Succès",
            text: response.data.message || "Tickets générés avec succès.",
          });
        } else {
          Swal.fire({
            icon: "error",
            title: "Erreur",
            text: response.data.error,
          });
        }
        getAllTickets();
      })
      .catch((error) => {
        Swal.fire({
          icon: "error",
          title: "Erreur",
          text:
            error.response?.data?.error ||
            "Une erreur est survenue lors de la génération des tickets.",
        });
      });
    setIsModalOpen(false);
    setTicketForm({ id_stop: 0, quantity: 0, price: 0 });
  };

  const filteredTickets = tickets.filter((t) => {
    const matchesSearch =
      t.stop?.name_stop?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.id_ticket?.toString().toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus =
      statusFilter === "" || t.status_ticket === statusFilter;

    return matchesSearch && matchesStatus;
  });

  const isAllSelected =
    filteredTickets.length > 0 &&
    filteredTickets.every((ticket) =>
      selectedTickets.includes(ticket.id_ticket),
    );

  const ticketsToPrint = filteredTickets.filter((ticket) =>
    selectedTickets.includes(ticket.id_ticket),
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
        <div className="flex flex-col sm:flex-row gap-3 w-full lg:w-auto flex-1">
          <div className="relative flex-1 max-w-md w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher par ID Ticket ou par Arrêt..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-gray-50 border border-gray-200 pl-10 pr-4 py-2 rounded-lg text-sm outline-none focus:border-[#3B3B98] focus:bg-white transition-colors"
            />
          </div>

          <div className="w-full sm:w-48 relative">
            <Filter className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full bg-gray-50 border border-gray-200 pl-10 pr-4 py-2 rounded-lg text-sm outline-none focus:border-[#3B3B98] focus:bg-white transition-colors appearance-none font-medium"
            >
              <option value="">Tous les statuts</option>
              <option value="UNUSED">Disponible</option>
              <option value="USED">Scanné</option>
              <option value="EXPIRED">Expiré</option>
            </select>
          </div>
        </div>

        <div className="flex items-center space-x-3 w-full lg:w-auto justify-end">
          {selectedTickets.length > 0 && (
            <button
              onClick={handleDeleteSelected}
              className="flex items-center space-x-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 text-xs font-bold uppercase tracking-wider rounded-lg shadow-sm transition-colors"
            >
              <Trash2 size={16} />
              <span>Supprimer ({selectedTickets.length})</span>
            </button>
          )}
          {filteredTickets.length > 0 && (
            <button
              onClick={handlePrint}
              disabled={selectedTickets.length === 0}
              className="flex items-center space-x-2 bg-gray-800 hover:bg-black disabled:bg-gray-400 disabled:cursor-not-allowed text-white px-4 py-2 text-xs font-bold uppercase tracking-wider rounded-lg shadow-sm transition-colors"
            >
              <Printer size={16} />
              <span>Imprimer ({selectedTickets.length})</span>
            </button>
          )}

          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center space-x-2 bg-[#3B3B98] hover:bg-[#19193E] text-white px-4 py-2 text-xs font-bold uppercase tracking-wider rounded-lg shadow-sm transition-colors"
          >
            <Plus size={16} />
            <span>Générer des tickets</span>
          </button>
        </div>
      </div>

      <Table
        title="Registre des Tickets et Validation QR"
        headers={[
          {
            label: (
              <input
                type="checkbox"
                checked={isAllSelected}
                onChange={handleSelectAll}
                className="w-4 h-4 accent-[#3B3B98] cursor-pointer"
              />
            ),
          },
          "Token",
          "Arrêt Associé",
          "Date de création",
          "Date d'expiration",
          "Date de validation",
          "Status",
          "Aperçu QR",
        ]}
      >
        {filteredTickets.length > 0 ? (
          filteredTickets.map((ticket, index) => (
            <tr
              key={ticket.id_ticket}
              className="hover:bg-gray-50 transition-colors"
            >
              <td className="px-6 py-4">
                <input
                  type="checkbox"
                  checked={selectedTickets.includes(ticket.id_ticket)}
                  onChange={() => handleSelectTicket(ticket.id_ticket)}
                  className="w-4 h-4 accent-[#3B3B98] cursor-pointer"
                />
              </td>

              <td className="px-6 py-4 font-mono font-bold text-xs text-[#3B3B98]">
                {ticket.num_ticket}
              </td>
              <td className="px-6 py-4 font-semibold text-gray-800">
                {ticket.stop.name_stop}
              </td>
              <td className="px-6 py-4 text-xs font-mono text-gray-400">
                {ticket.created_at}
              </td>
              <td className="px-6 py-4 text-xs font-mono text-gray-400">
                {ticket.expired_at}
              </td>
              <td className="px-6 py-4 text-xs font-mono text-gray-400">
                {ticket.used_at || "Non validé"}
              </td>
              <td className="px-6 py-4">
                {ticket.status_ticket === "UNUSED" && (
                  <span className="text-[10px] px-2.5 py-1 rounded-full font-bold inline-flex items-center bg-green-100 text-green-700">
                    DISPONIBLE
                  </span>
                )}
                {ticket.status_ticket === "USED" && (
                  <span className="text-[10px] px-2.5 py-1 rounded-full font-bold inline-flex items-center bg-gray-100 text-gray-400 line-through">
                    SCANNÉ
                  </span>
                )}
                {ticket.status_ticket === "EXPIRED" && (
                  <span className="text-[10px] px-2.5 py-1 rounded-full font-bold inline-flex items-center bg-gray-100 text-red-400">
                    EXPIRE
                  </span>
                )}
              </td>
              <td className="px-6 py-2 text-center">
                <button
                  type="button"
                  onClick={() => setSelectedQR(ticket.qr_code)}
                  className="bg-white p-2 rounded-lg border inline-flex cursor-pointer hover:shadow-md hover:scale-105 transition"
                  title="Agrandir le QR Code"
                >
                  <QRCode
                    value={ticket.qr_code}
                    size={70}
                    bgColor="#FFFFFF"
                    fgColor="#000000"
                    level="H"
                  />
                </button>
              </td>
            </tr>
          ))
        ) : (
          <tr>
            <td colSpan="8" className="text-center py-4 text-gray-500">
              Aucun ticket trouvé.
            </td>
          </tr>
        )}
      </Table>

      <div ref={printRef} className="print-container">
        <div className="grid grid-cols-2 gap-4 p-4">
          {ticketsToPrint.map((ticket) => (
            <TicketCard key={ticket.id_ticket} ticket={ticket} />
          ))}
        </div>
      </div>

      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
      />

      {isModalOpen && (
        <TicketForm
          stops={stops}
          ticketForm={ticketForm}
          setTicketForm={setTicketForm}
          handleGenerateTickets={handleGenerateTickets}
          isModalOpen={isModalOpen}
          setIsModalOpen={setIsModalOpen}
        />
      )}

      {isEdit && (
        <TicketForm
          stops={stops}
          ticketForm={ticketForm}
          setTicketForm={setTicketForm}
          handleGenerateTickets={handleGenerateTickets}
          isModalOpen={isModalOpen}
          setIsModalOpen={setIsModalOpen}
        />
      )}

      {selectedQR && (
        <div
          className="fixed inset-0 z-60 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
          onClick={() => setSelectedQR(null)}
        >
          <div
            className="relative bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Bouton fermer */}
            <button
              type="button"
              onClick={() => setSelectedQR(null)}
              className="absolute top-3 right-3 w-8 h-8 flex items-center justify-center rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 text-xl"
            >
              ×
            </button>

            <h3 className="text-lg font-semibold text-gray-800 text-center mb-4">
              QR Code du ticket
            </h3>

            {/* QR Code agrandi */}
            <div className="flex justify-center">
              <div className="bg-white p-4 rounded-xl border">
                <QRCode
                  value={selectedQR}
                  size={300}
                  bgColor="#FFFFFF"
                  fgColor="#000000"
                  level="H"
                />
              </div>
            </div>

            <p className="text-sm text-gray-500 text-center mt-4">
              Scannez ce QR Code avec l'application mobile
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
