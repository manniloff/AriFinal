/**
 * 
 */
package com.unifun.sigtran.faces;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import com.unifun.sigtran.adaptor.SigtranStackBean;

/**
 * @author rbabin
 *
 */
@ManagedBean
public class StackReloadBean {
	@ManagedProperty("#{sigtranStackBean}")
	private SigtranStackBean sigStack;
	
	public void reloadAction(ActionEvent actionEvent) {
		try {
		sigStack.getStack().stop();
		InputStream fstream = this.getClass().getClassLoader().getResourceAsStream("config.txt");
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		sigStack.getStack().readConfig(br);
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Config Reloaded, Please also restart all aplication related to SIGTRAN Stack",  null);
        FacesContext.getCurrentInstance().addMessage(null, message);
		} catch (Exception e) {
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(),  null);
	        FacesContext.getCurrentInstance().addMessage(null, message);
			e.printStackTrace();
		}
			
    }

	public SigtranStackBean getSigStack() {
		return sigStack;
	}

	public void setSigStack(SigtranStackBean sigStack) {
		this.sigStack = sigStack;
	}

}
