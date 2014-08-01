package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.Serializable;
import java.util.Arrays;

public class MessageBody implements Serializable{
	
	public int sequenceNumber;
	public String message;
	public int sender;
	public String messageId; // concat of AVDNumber and timestamp
	public int[] vectorClock;
	public String messageType; //"reg" or "order"
	
	public MessageBody(String mId,String type,String body,int sender,int[] vClock) 
	{
		// TODO Auto-generated constructor stub
		this.messageId=mId;
		this.message=body;
		this.messageType=type;
		this.sender=sender;
		this.vectorClock=Arrays.copyOf(vClock, vClock.length);
		this.sequenceNumber=-1;
		
	}
	
	public void setSequencenumber(int n)
	{
		sequenceNumber=n;
	}
	

}
