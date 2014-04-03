/*
* Copyright: (c) Mayo Foundation for Medical Education and
* Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
* triple-shield Mayo logo are trademarks and service marks of MFMER.
*
* Distributed under the OSI-approved BSD 3-Clause License.
* See http://ncip.github.com/lexevs-service/LICENSE.txt for details.
*/
package edu.mayo.cts2.framework.plugin.service.lexevs;

import gov.nih.nci.system.client.ApplicationServiceProvider;

import java.util.Map;

import org.LexGrid.LexBIG.Impl.LexBIGServiceImpl;
import org.LexGrid.LexBIG.LexBIGService.LexBIGService;
import org.LexGrid.LexBIG.caCore.interfaces.LexEVSApplicationService;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A factory for creating LexBigService objects.
 */
public class LexBigServiceFactory implements FactoryBean<LexBIGService>, DisposableBean {

	private static final String LG_CONFIG_FILE_ENV = "LG_CONFIG_FILE";
	
	protected Logger log = Logger.getLogger(this.getClass());

	private LexBIGService lexBIGService;

	private String lexevsRemoteApiUrl;

	private Boolean useRemoteApi;
	
	private String lgConfigFile;
	
	private boolean hasBeenConfigured = false;

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public LexBIGService getObject() throws Exception {
		while(! this.hasBeenConfigured){
			this.log.warn("Waiting for the Configuration Service to start...");
			Thread.sleep(4000);
		}
		if (BooleanUtils.toBoolean(this.useRemoteApi) && StringUtils.isNotBlank(this.lexevsRemoteApiUrl)) {
			return (LexEVSApplicationService) ApplicationServiceProvider
					.getApplicationServiceFromUrl(this.lexevsRemoteApiUrl, "EvsServiceInfo");
		} else {
			if(StringUtils.isBlank(this.lgConfigFile)){
				throw new IllegalStateException(LG_CONFIG_FILE_ENV + " value is empty.");
			}
			System.setProperty(LG_CONFIG_FILE_ENV, this.lgConfigFile);
			
			this.lexBIGService = LexBIGServiceImpl.defaultInstance();
			return this.lexBIGService;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return LexBIGService.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}
	
	@Override
	public void destroy() throws Exception {
		if(this.lexBIGService != null){
			log.info("Shutting down local LexEVS.");
			this.lexBIGService.getServiceManager(null).shutdown();
		}
	}
	
	public void updateCallback(Map<String,?> properties){
        try {
            Context context = (Context) new InitialContext().lookup("java:/comp/env");
            this.lexevsRemoteApiUrl = (String) context.lookup("lexevsRemoteApiUrl");
            this.lgConfigFile = (String) context.lookup("lexevsConfigFile");
            this.useRemoteApi = (Boolean) context.lookup("lexevsRemoteApi");

            this.hasBeenConfigured = true;
        } catch (NamingException ne) {
            log.fatal("Unable to get the configuration from the context.", ne);
        }
	}

	public String getLexevsRemoteApiUrl() {
		return lexevsRemoteApiUrl;
	}

	public void setLexevsRemoteApiUrl(String lexevsRemoteApiUrl) {
		this.lexevsRemoteApiUrl = lexevsRemoteApiUrl;
	}

	public Boolean getUseRemoteApi() {
		return useRemoteApi;
	}

	public void setUseRemoteApi(Boolean useRemoteApi) {
		this.useRemoteApi = useRemoteApi;
	}

	public String getLgConfigFile() {
		return lgConfigFile;
	}

	public void setLgConfigFile(String lgConfigFile) {
		this.lgConfigFile = lgConfigFile;
	}

}
