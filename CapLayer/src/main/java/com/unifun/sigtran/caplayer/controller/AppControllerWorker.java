//package com.unifun.sigtran.caplayer.controller;
//
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import javax.servlet.AsyncContext;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.unifun.sigtran.adaptor.SigtranStackBean;
//import com.unifun.sigtran.beepcall.utils.Channel;
//import com.unifun.sigtran.beepcall.utils.Channel.CircuitStates;
//import com.unifun.sigtran.caplayer.ISUPEventHandler;
//
//public class AppControllerWorker implements Runnable {
//	static final Logger logger = LoggerFactory.getLogger(String.format("[%1$-15s] %2$s", IsupCallWorker.class.getSimpleName(), ""));
//	private AsyncContext asyncContext;	
//	private HashMap<String, String> params = new HashMap<>();
//	private PrintWriter out;
//	private SigtranStackBean sigtranStack;
////	private ISUPEventHandler isupEventHandler;
//	@Override
//	public void run() {
//		asyncContext.getResponse().setContentType("text/plain;charset=UTF-8");
//		//Load parameters into hashmap
//		Enumeration<String> parameterNames = asyncContext.getRequest().getParameterNames();
//		while (parameterNames.hasMoreElements()) {
//			String paramName = parameterNames.nextElement();
//			for (String value : asyncContext.getRequest().getParameterValues(paramName)) {                
//				params.putIfAbsent(paramName, value);
//			}
//		}
//		//setup printer
//		try {
//			setOut(asyncContext.getResponse().getWriter());
//		} catch (IOException e1) {			
//			logger.error(e1.getMessage());
//			e1.printStackTrace();
//			closeResource();
//			return;
//		}
//		//lookup for sigtranstack and isup handler
//		sigtranStack = (SigtranStackBean) asyncContext.getRequest().getServletContext().getAttribute("sigtranStack");
////		isupEventHandler = (ISUPEventHandler) asyncContext.getRequest().getServletContext().getAttribute("isupEventHandler");
////		if (sigtranStack==null || isupEventHandler == null){
////			out.print("{\"Status\":\"-1\", \"Error\":\"Unable to obtain sigtran stack or isup event handler\"}");
////			closeResource();
////			return;
////		}
//		//Validate Parameters
//		if(!params.containsKey("action")){
//			out.print("{\"Status\":\"-1\", \"Error\":\"Missing action parameter\"}");
//			closeResource();
//			return;
//		}
//		switch (params.get("action")){
//		case "channelsstatus":
//			printChannelsStatus();
//			break;
//		case "channelscountstates":
//			printChannelsStatesCount();
//			break;
//		case "resetallchannels":
//			resetAllChannels();
//			break;
//		case "resetchannel":
//			resetChannel();
//			break;
//		case "unblock":
//			unblockChannel();
//			break;
//		default:
//			out.print("{\"Status\":\"-1\", \"Error\":\"Unknown action: "+params.get("action")+"\"}");
//			closeResource();
//			break;
//		}
//		closeResource();
//		
//		
//	}
//	
//	/**
//	 * 
//	 */
//	private void resetChannel() {
//		if(!this.params.containsKey("cic")){
//			this.out.print("{\"Status\":\"-1\", \"Error\":\"Please provide cic which should bee reseted\"}");			
//			return;
//		}
//		String cicStr = this.params.get("cic");
//		if (!cicStr.matches("[0-9]+")){
//			out.print(String.format("{\"Status\":\"-1\", \"Error\":\"cic value should be a numbers\"}"));			
//			return;
//		}
//		if(!this.params.containsKey("dpc")){
//			this.out.print("{\"Status\":\"-1\", \"Error\":\"Please provide the dpc\"}");			
//			return;
//		}
//		String dpcStr = this.params.get("dpc");
//		if (!dpcStr.matches("[0-9]+")){
//			out.print(String.format("{\"Status\":\"-1\", \"Error\":\"dpc value should be a numbers\"}"));			
//			return;
//		}
//		int cic = Integer.parseInt(cicStr);
//		int dpc=  Integer.parseInt(dpcStr) ;
//		long channelId = this.isupEventHandler.getStack().getCircuitManager().getChannelID(cic,dpc);
//		this.isupEventHandler.sendRSC(cic, dpc, channelId);
//		out.print(String.format("{\"Status\":\"0\", \"Message\":\"RSC sent to cic: %, dpc: %d, channelId: %d\"}",cic,dpc,channelId));
//		
//		
//	}
//	
//	private void unblockChannel() {
//		if(!this.params.containsKey("cic")){
//			this.out.print("{\"Status\":\"-1\", \"Error\":\"Please provide cic which should bee reseted\"}");			
//			return;
//		}
//		String cicStr = this.params.get("cic");
//		if (!cicStr.matches("[0-9]+")){
//			out.print(String.format("{\"Status\":\"-1\", \"Error\":\"cic value should be a numbers\"}"));			
//			return;
//		}
//		if(!this.params.containsKey("dpc")){
//			this.out.print("{\"Status\":\"-1\", \"Error\":\"Please provide the dpc\"}");			
//			return;
//		}
//		String dpcStr = this.params.get("dpc");
//		if (!dpcStr.matches("[0-9]+")){
//			out.print(String.format("{\"Status\":\"-1\", \"Error\":\"dpc value should be a numbers\"}"));			
//			return;
//		}
//		int cic = Integer.parseInt(cicStr);
//		int dpc=  Integer.parseInt(dpcStr) ;
//		long channelId = this.isupEventHandler.getStack().getCircuitManager().getChannelID(cic,dpc);
//		this.isupEventHandler.sendUBL(cic, dpc, channelId);
//		out.print(String.format("{\"Status\":\"0\", \"Message\":\"UBL sent to cic: %, dpc: %d, channelId: %d\"}",cic,dpc,channelId));
//		
//	}
//
//	private void resetAllChannels() {
//		this.isupEventHandler.resetAllChannels();
//		out.print("{\"Status\":\"0\", \"Message\":\"Reset All Channels SIG was sent to stack\"}");
//		
//	}
//
//	private void printChannelsStatus() {
//		StringBuffer buff = new StringBuffer();
//		buff.append("{ChannelsStatus: [\n");
//		ConcurrentHashMap<Long, Channel> channels = isupEventHandler.getCicMgm().getChannels();
//		List<Long> sortKeys = Collections.list(channels.keys());
//		Collections.sort(sortKeys);
//		sortKeys.forEach(channelId -> {
//			Channel ch = channels.get(channelId);
//			buff.append(String.format("{\"channelid\":%d, "
//					+ "\"cic\":%d, "
//					+ "\"dpc\":%d, "
//					+ "\"state\":\"%s\"},\n", 
//					ch.getChannelId(),
//					ch.getCic(),
//					ch.getDpc(),
//					ch.getState()));
//		});
//		buff.append("{}]}");
//		getOut().print(buff.toString());
//	}
//	
//	private void printChannelsStatesCount() {
//		StringBuffer buff = new StringBuffer();
//		buff.append("{ChannelsStatesCount: \n");
//		ConcurrentHashMap<Long, Channel> channels = isupEventHandler.getCicMgm().getChannels();
//		Map<CircuitStates, Long> channelStatesCount = new HashMap<>();
//		channels.forEach((channelId, channel) -> {			
//			if (channelStatesCount.containsKey(channel.getState())){
//				Long count = channelStatesCount.get(channel.getState())+1;
//				channelStatesCount.put(channel.getState(), count);
//			}else{
//				channelStatesCount.put(channel.getState(), 1L);
//			}
//		});
//		List<String> channelsState = new ArrayList<>();
//		for (CircuitStates cs : channelStatesCount.keySet()){
//			channelsState.add(String.format("{\"state\":\"%s\",\"count\":\"%d\"}", cs, channelStatesCount.get(cs)));
//		}
//		buff.append(String.format("%s}",channelsState.toString()));
//		getOut().print(buff.toString());
//	}
//
//	private void closeResource(){
//		asyncContext.complete();
//	}
//	
//	public void setAsyncContext(AsyncContext asyncContext) {
//		this.asyncContext = asyncContext;
//	}
//	public PrintWriter getOut() {
//		return out;
//	}
//
//	public void setOut(PrintWriter out) {
//		this.out = out;
//	}
//
//}
