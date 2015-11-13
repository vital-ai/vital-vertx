package ai.vital.adminauth.mod

import ai.vital.auth.mod.VitalAuthManager
import ai.vital.auth.mod.VitalAuthManager.AuthAppBean



class VitalAdminAuthManager extends VitalAuthManager {

	public final static String admin_address_login     = "admin.vitalauth.login"
	
	public final static String admin_address_logout    = "admin.vitalauth.logout"
	
	public final static String admin_address_authorise = "admin.vitalauth.authorise"

	/*	

	*/
	@Override
	protected String getAddress_login() {
		return admin_address_login
	}

	@Override
	protected String getAddress_logout() {
		return admin_address_logout
	}

	@Override
	protected String getAddress_authorise() {
		return admin_address_authorise
	}

	
	@Override
	protected AuthAppBean createBean(String access) {
		if(access == 'service') {
			return new AuthAppBean()
		}
		if(access == 'admin') {
			return new AdminAuthAppBean()
		}
		
		throw new RuntimeException("Unsupported access: ${access}")
	}
	
	


		
	
}
