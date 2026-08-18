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

  async getIncomeByBus(bus_id: number) {
    return await prisma.income.aggregate({
      where: { bus_id: bus_id },
      _sum: { amount: true },
    });
  }

  async filterIncomeByDate(bus_id: any) {
    const now = new Date();

    const year = now.getFullYear();
    const month = now.getMonth();

    const startDate = new Date(year, month, 1);
    const endDate = new Date(year, month + 1, 1);

    // console.log("Bus ID:", bus_id);

    // Cas "month"
    if (bus_id === "month") {
      const result = await prisma.income.aggregate({
        where: {
          date_in: {
            gte: startDate,
            lt: endDate,
          },
        },
        _sum: {
          amount: true,
        },
      });

      return result._sum.amount ?? 0;
    }

    // Cas "day"
    if (bus_id === "day") {
      const today = new Date();

      const startOfDay = new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate(),
      );

      const endOfDay = new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate() + 1,
      );

      const result = await prisma.income.aggregate({
        where: {
          date_in: {
            gte: startOfDay,
            lt: endOfDay,
          },
        },
        _sum: {
          amount: true,
        },
      });

      return result._sum.amount ?? 0;
    }

    // Cas bus_id
    const busId = parseInt(bus_id);

    if (!isNaN(busId)) {
      const result = await prisma.income.aggregate({
        where: {
          bus_id: busId,
          date_in: {
            gte: startDate,
            lt: endDate,
          },
        },
        _sum: {
          amount: true,
        },
      });

      return result._sum.amount ?? 0;
    }

    return 0;
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
