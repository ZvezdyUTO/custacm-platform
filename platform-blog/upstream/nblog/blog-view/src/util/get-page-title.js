const BRAND_NAME = 'custacm-platpform'

export default function getPageTitle(pageTitle) {
	if (pageTitle) {
		return `${pageTitle} - ${BRAND_NAME}`
	}
	return BRAND_NAME
}
