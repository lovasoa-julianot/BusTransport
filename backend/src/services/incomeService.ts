import { prisma } from "../client";

export class IncomeService {
  async createIncome(data: any) {
    return await prisma.income.create({ data });
  }

  async getAllIncomes() {
    return await prisma.income.aggregate({
      _sum: {
        amount: true,
      },
    });
  }

  async getIncomeByDriver(driver_id: any) {
    const tickets = await prisma.ticket.findMany({
      where: {
        driver_id: Number(driver_id),
      },
      select: {
        id_ticket: true,
      },
    });

    if (tickets.length === 0) {
      return {
        total_income: 0,
      };
    }

    const ticketIds = tickets.map((ticket) => ticket.id_ticket);

    const result = await prisma.income.aggregate({
      where: {
        ticket_id: {
          in: ticketIds,
        },
      },
      _sum: {
        amount: true,
      },
    });

    return {
      total_income: result._sum.amount ?? 0,
    };
  }

  async getTotalIncome() {
    const result = await prisma.income.aggregate({
      _sum: {
        amount: true,
      },
    });

    return result._sum.amount ?? 0;
  }

  async filterIncomeByDate(bus_id: any, period: string) {
    const now = new Date();

    let startDate: Date;
    let endDate: Date;

    if (period === "day") {
      startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());

      endDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    } else if (period === "month") {
      startDate = new Date(now.getFullYear(), now.getMonth(), 1);

      endDate = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    } else {
      throw new Error("Période invalide.");
    }

    const where: any = {
      date_in: {
        gte: startDate,
        lt: endDate,
      },
    };

    if (bus_id !== undefined && bus_id !== null && bus_id !== "all") {
      const busId = Number(bus_id);

      if (isNaN(busId)) {
        throw new Error("ID du bus invalide.");
      }

      where.bus_id = busId;
    }

    const result = await prisma.income.aggregate({
      where,
      _sum: {
        amount: true,
      },
    });

    return result._sum.amount ?? 0;
  }

  async updateIncome(id_income: number, data: any) {
    return await prisma.income.update({
      where: { id_in: id_income },
      data,
    });
  }

  async deleteIncome(id_income: number) {
    return await prisma.income.delete({
      where: { id_in: id_income },
    });
  }
}
