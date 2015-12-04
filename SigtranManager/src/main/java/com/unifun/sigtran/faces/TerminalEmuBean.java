/**
 * 
 */
package com.unifun.sigtran.faces;

import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import com.unifun.sigtran.adaptor.SigtranStackBean;

/**
 * @author rbabin
 *
 */
@ManagedBean
public class TerminalEmuBean {
	@ManagedProperty("#{sigtranStackBean}")
	private SigtranStackBean sigStack; 
	public String handleCommand(String command, String[] params) {
        if(command.equals("ss7")) {
            if(params.length > 0)
                return execCmd(params);
            else
                return "please type help to see available options";
        }
        else if(command.equals("help"))
            return showHelp(params);       
        else
            return "'"+command + "' is not recognized as an internal command";
    }
	private String execCmd(String[] args){
		String respString;	
		args[0] = args[0].trim();
		if (args[0].compareTo("") != 0) {
			if (args[0].compareTo("#") != 0) {
				if (args[0].compareTo(" ") != 0) {
					if (!args[0].trim().startsWith("#")) {
						
						if (args[0].compareToIgnoreCase("SCTP") == 0)
						{							
							respString = sigStack.getStack().getSctp().getSctpShellExecuter().execute(args);//this.sctpShellExecuter.execute(args);
							return respString;
						}
						else if (args[0].compareToIgnoreCase("M3UA") == 0)
						{							
							respString = sigStack.getStack().getM3ua().getM3uaShellExecuter().execute(args);
							return respString;							
						}
						else if (args[0].compareToIgnoreCase("SCCP") == 0)
						{
							respString = sigStack.getStack().getSccp().getSccpShellExecuter().execute(args);//this.sccpShellExecuter.execute(args);
							return respString;
						}
						else if (args[0].compareToIgnoreCase("ISUP") == 0)
						{							
							respString = sigStack.getStack().getIsup().getIsupShellExecutor().execute(args);//this.isupShellExecuter.execute(args);
							return respString;
						}
						else
						{
							return "invalid command string found";
						}
					}
				}
			}
		}
		return "nothing to do";
	}
	
	/**
	 * @param params
	 * @return
	 */
	private String showHelp(String[] params) {
		// TODO Auto-generated method stub
		return "TODO: Write help commands here";
	}
	public SigtranStackBean getSigStack() {
		return sigStack;
	}
	public void setSigStack(SigtranStackBean sigStack) {
		this.sigStack = sigStack;
	}
}
