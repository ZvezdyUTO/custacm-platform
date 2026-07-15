// Author: huangbingrui.awa

const ISO_LOCAL_DATE = /^(\d{4})-(\d{2})-(\d{2})$/

function normalizedYear(value) {
	if (value === null || value === undefined || value === '') return ''
	const year = Number(value)
	return Number.isInteger(year) && year >= 1 && year <= 9999 ? String(year).padStart(4, '0') : ''
}

function isLeapYear(year) {
	return year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
}

function validCalendarDate(year, month, day) {
	const daysByMonth = [31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
	return month >= 1 && month <= 12 && day >= 1 && day <= daysByMonth[month - 1]
}

export function competitionDatePresentation(competition) {
	const rawDate = typeof competition?.competitionDate === 'string'
		? competition.competitionDate.trim()
		: ''
	const match = ISO_LOCAL_DATE.exec(rawDate)
	if (match) {
		const year = Number(match[1])
		const month = Number(match[2])
		const day = Number(match[3])
		if (year >= 1 && validCalendarDate(year, month, day)) {
			return {
				datetime: rawDate,
				hasExactDate: true,
				isKnown: true,
				label: `${year}年${month}月${day}日`,
				monthDay: `${match[2]}.${match[3]}`,
				year: match[1],
			}
		}
	}

	const year = normalizedYear(competition?.year)
	if (year) {
		return {
			datetime: year,
			hasExactDate: false,
			isKnown: true,
			label: `${Number(year)}年`,
			monthDay: '',
			year,
		}
	}

	return {
		datetime: '',
		hasExactDate: false,
		isKnown: false,
		label: '日期待补充',
		monthDay: '',
		year: '—',
	}
}
