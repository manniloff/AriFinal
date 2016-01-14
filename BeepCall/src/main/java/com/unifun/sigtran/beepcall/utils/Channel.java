/**
 * 
 */
package com.unifun.sigtran.beepcall.utils;

import java.sql.Timestamp;

/**
 * @author rbabin
 *
 */
public class Channel {
	public enum CircuitStates {
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
		  ST_GOT_CPG,
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
		  ST_GOT_RSC,
		  ST_SENT_RSC,
		  ST_SENT_GRS,
		  ST_BUSY,
		};
	protected CircuitStates state;
	protected int cic;
	protected int dpc;
	protected long channelId;
	protected String callingParty;
	protected String calledParty;
	protected int causeIndicator;
	protected Timestamp statrtDate;
	protected Timestamp endDate;
	int sessionId;
	

	public Channel(int cic, int dpc, long channelId) {
		this.state = CircuitStates.ST_CONCHECK;
		this.cic = cic;
		this.dpc = dpc;
		this.channelId=channelId;
	
	}
	
	public CircuitStates getState() {
		return state;
	}


	public void setState(CircuitStates state) {
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
		return "Channel: CIC=" + cic + "DPC="+dpc+" State=" + this.state;
	}

	public int getDpc() {
		return dpc;
	}

	public void setDpc(int dpc) {
		this.dpc = dpc;
	}

	public long getChannelId() {
		return channelId;
	}

	public void setChannelId(long channelId) {
		this.channelId = channelId;
	}

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public String getCallingParty() {
		return callingParty;
	}

	public void setCallingParty(String callingParty) {
		this.callingParty = callingParty;
	}

	public String getCalledParty() {
		return calledParty;
	}

	public void setCalledParty(String calledParty) {
		this.calledParty = calledParty;
	}

	public int getCauseIndicator() {
		return causeIndicator;
	}

	public void setCauseIndicator(int causeIndicator) {
		this.causeIndicator = causeIndicator;
	}

	public Timestamp getStatrtDate() {
		return statrtDate;
	}

	public void setStatrtDate(Timestamp statrtDate) {
		this.statrtDate = statrtDate;
	}

	public Timestamp getEndDate() {
		return endDate;
	}

	public void setEndDate(Timestamp endDate) {
		this.endDate = endDate;
	}


}


