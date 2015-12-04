/**
 * 
 */
package com.unifun.sigtran.beepcall.utils;

/**
 * @author rbabin
 *
 */
public class Channel {
	public enum Circuit_states {
		  /* Circuit idle, ready to accept or initiate calls. */
		  ST_IDLE,
		  /* An IAM has been received, but no ACM or CON has been sent back yet. */
		  ST_GOT_IAM,
		  /* An IAM has been sent to initiate a call, but no ACM or CON has been
		     received back yet. */
		  ST_SENT_IAM,
		  /* We have sent an ACM and have to send an ANM now */
		  ST_SENT_ACM,
		  /* We have sent IAM and received ACM, so waiting for ANM. */
		  ST_GOT_ACM,
		  /* A call is connected (incoming or outgoing). */
		  ST_CONNECTED,
		  /* A continuity check is ongoing */
		  ST_CONCHECK,
		  /* A REL message has been received, but RLC has not been sent
		     yet. ast_softhangup() has been called on the channel.*/
		  ST_GOT_REL,
		  /* A REL has been sent (from ss7_hangup), and so the struct ast_channel *
		     has been deallocated, but the circuit is still waiting for RLC to be
		     received before being able to initiate new calls. If timer T16 or T17
		     is running, this state instead means that a "circuit reset" has been
		     sent, and we are waiting for RLC. If a REL is received in this state,
		     send RLC and stay in this state, still waiting for RLC */
		  ST_SENT_REL,
		  ST_BUSY,
		};
	protected Circuit_states state;
	protected int cic;
	protected int dpc;
	
	
	public Channel(int cic, int dpc) {
		this.state = Circuit_states.ST_IDLE;
		this.cic = cic;
		this.dpc = dpc;
	
	}
	
	public Circuit_states getState() {
		return state;
	}


	public void setState(Circuit_states state) {
		this.state = state;
	}


	public int getCic() {
		return cic;
	}


	public void setCic(int cic) {
		this.cic = cic;
	}
	
	@Override 
	public String toString(){
		return "Channel: CIC=" + cic + " State=" + this.state;
	}

	public int getDpc() {
		return dpc;
	}

	public void setDpc(int dpc) {
		this.dpc = dpc;
	}

}


