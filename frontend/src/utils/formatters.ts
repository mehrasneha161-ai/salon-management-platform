import dayjs from 'dayjs'

export const formatDate = (date: string): string =>
  dayjs(date).format('DD MMM YYYY')

export const formatDateTime = (date: string): string =>
  dayjs(date).format('DD MMM YYYY, hh:mm A')

export const formatCurrency = (amount: number): string =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount)

export const getStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    PENDING: 'orange',
    SLOT_LOCKED: 'blue',
    CONFIRMED: 'green',
    IN_PROGRESS: 'purple',
    COMPLETED: 'success',
    CANCELLED: 'red',
    REJECTED: 'error',
  }
  return colors[status] ?? 'default'
}
