package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Vector;
import java.util.concurrent.Executors;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */

/* Logic for the code :
 * 
 * 	AVD0 is the sequencer.
 *  The sequencer implements causal ordering.
 *  All the others implement total ordering (thanks to the sequencer)
 *  
 *  --- Subhranil
 *  
 *  */
public class GroupMessengerActivity extends Activity {

    
    private String myPortStr;
    public int AVDNumber;
//    static final String REMOTE_PORT0 = "11108";
//    static final String REMOTE_PORT1 = "11112";
    public final int SERVER_PORT = 10000;
    public final String[] ports={"11108","11112","11116","11120","11124"};
    private PriorityQueue<MessageBody> deliverQueue;
    private Hashtable<String, MessageBody> waitingQ;
    private Vector<MessageBody> sequencerWaitingQ;
    public int[] vClock={0,0,0,0,0};
    private int[] vClockForSequencer={0,0,0,0,0};
    private int localSqNumber; // for each process
    private int globalSqNumber; // for the sequencer
    public int numberOfNodes=5;
    public String TAG = GroupMessengerActivity.class.getSimpleName();  
      
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        
        
        init();

        //setup the sever
        try {
            ServerSocket serverSocket = new ServerSocket(GroupMessengerActivity.this.SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.d(TAG, "Can't create a ServerSocket");
            return;
        }
        
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        
        
        Button sendButton=(Button)findViewById(R.id.button4);
        sendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub // write sending code here 
				String message=((EditText)findViewById(R.id.editText1)).getText().toString();
				((EditText)findViewById(R.id.editText1)).setText(""); 
				GroupMessengerActivity g=GroupMessengerActivity.this;
				String id=String.valueOf(g.AVDNumber)+String.valueOf(System.currentTimeMillis() / 1000L);
				g.vClock[g.AVDNumber]++;
				MessageBody m=new MessageBody(id, "reg", message, g.AVDNumber,g.vClock );
				new ClientThread(m,g.ports).start();
                // new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, "");
			}
		});
        
    }

    
    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * 
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     * 
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try
            {
            	while(true) {
            	Socket serverSock=serverSocket.accept();
            	ObjectInputStream oStream=new ObjectInputStream(serverSock.getInputStream());
            	MessageBody m=(MessageBody)oStream.readObject();
            	
            	Executors.newSingleThreadExecutor().execute(new MessageHandler(m));
            	
//            	final String textReceived=m.message;//.readLine();
//            	runOnUiThread(new Runnable() {
//					
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//						onProgressUpdate(textReceived);
//					}
//				});
            	
            	oStream.close();
            	serverSock.close();
            	}
            }
            catch(Exception e)
            {
            	Log.d(TAG, e.getStackTrace().toString());
            	//System.out.println(e);
            }
            return null;
        }
        
  
   }
    
    public void onProgressUpdate(String...strings) {
        /*
         * The following code displays what is received in doInBackground().
         */
        String strReceived = strings[0].trim();
        TextView remoteTextView = (TextView) findViewById(R.id.textView1);
        remoteTextView.append(strReceived + "\t\n");
        TextView localTextView = (TextView) findViewById(R.id.textView1);
        localTextView.append("\n");
        
        /*
         * The following code creates a file in the AVD's internal storage and stores a file.
         * 
         * For more information on file I/O on Android, please take a look at
         * http://developer.android.com/training/basics/data-storage/files.html
         */
        
//        String filename = "SimpleMessengerOutput";
//        String string = strReceived + "\n";
//        FileOutputStream outputStream;
//
//        try {
//            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//            outputStream.write(string.getBytes());
//            outputStream.close();
//        } catch (Exception e) {
//            Log.e(TAG, "File write failed");
//        }
        return;
    }

    
    
    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     * 
     * @author stevko
     *
     */
    
    public class ClientThread extends Thread
    {
    	private MessageBody mssg;
    	private String[] ports;
    	public ClientThread(MessageBody m,String ports[])
    	{
    		mssg=m;
    		this.ports=ports;
    	}
    	public void run()
    	{

            try {
//                String remotePort = REMOTE_PORT0;
//                if (msgs[1].equals(REMOTE_PORT0))
//                    remotePort = REMOTE_PORT1; 

            	for(int i=0;i<GroupMessengerActivity.this.numberOfNodes;i++)
            	{
            		int port=Integer.parseInt(ports[i]);
            		Log.d(TAG,"Trying with port:"+String.valueOf(port));
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),port);
                                  
                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                    ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
                    output.writeObject(mssg);
                    output.close();
                    socket.close();
            		
            	}
            } catch (UnknownHostException e) {
                Log.d(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.d(TAG, "ClientTask socket IOException");
            }

            
        
    	}
    }
    
//    private class ClientTask extends AsyncTask<String, Void, Void> {
//
//        @Override
//        protected Void doInBackground(String... msgs) {}
//    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
  
    
    // Rest of the code is new and added by Subhranil

    public void init()
    {
    	getAVDDetails();
    	Comparator<MessageBody> comp=new MyComparator();
    	deliverQueue=new PriorityQueue<MessageBody>(50, comp);
    	waitingQ=new Hashtable<String, MessageBody>();
    	sequencerWaitingQ=new Vector<MessageBody>();
    	localSqNumber=0;
    	globalSqNumber=0;
    	TAG=TAG+String.valueOf(AVDNumber);
    	
    }
    
    
    public void getAVDDetails()
    {
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPortStr = String.valueOf((Integer.parseInt(portStr) * 2));
        AVDNumber=Arrays.asList(ports).indexOf(myPortStr);
    }
    
    public class MessageHandler implements Runnable
    {
    	private MessageBody message;
    	public MessageHandler(MessageBody m)
    	{
    		message=m;
    	}
    	
    	public void run()
    	{
    		handleIncomingMessage(message);
    	}
    	
        //probably the most important function .
        public void handleIncomingMessage(MessageBody message)
        {
//        	if(true)
//        	{
//        		System.out.println("Going out:"+AVDNumber);
//        		return;
//        	}
        	
        	//Log.d(TAG, "Handling"+message.message+"at avd index:"+String.valueOf(AVDNumber));
        	if(message.messageType.equals("order")) // message from sequencer
        	{
        		MessageBody m=waitingQ.get(message.messageId);
        		//Log.d(TAG, "No message found with id:"+message.messageId);
        		if(m!=null)        			
        		{	m.setSequencenumber(message.sequenceNumber);
        			deliverQueue.add(m);    
        		}
        	}
        	else //regular message
        	{
        		waitingQ.put(message.messageId, message);
        		if(AVDNumber!=4) // non  sqequncer
        		{
        			for(int i=0;i<numberOfNodes;i++)
        				vClock[i]=Math.max(vClock[i], message.vectorClock[i]);
        		}
        		else // perform the whole sequencer operation , check for causality and then send the order message
	        	{
	        		try 
	        		{
	        			Thread.sleep(2);
	    				if(message.vectorClock[message.sender]==(vClockForSequencer[message.sender]+1))
	    				{
	    					vClockForSequencer[message.sender]+=1;//Math.max(vClockForSequencer[message.sender],message.vectorClock[message.sender]);
	    					//globalSqNumber++;
	    					MessageBody m=new MessageBody(message.messageId, "order", "" , AVDNumber, vClockForSequencer);
	    					m.setSequencenumber(globalSqNumber++);
	    					new ClientThread(m,ports).start();
	    				}
	    				else 
	    					sequencerWaitingQ.add(message);
	    				// Iterate over all waiting messages to check for causality
	    				
	    				if (sequencerWaitingQ.size() > 0) {
	    					Iterator<MessageBody> it = sequencerWaitingQ.iterator();
	    					while (it.hasNext())
	    					{
	    						MessageBody mess=it.next();
	    						if (mess.vectorClock[mess.sender]==(vClockForSequencer[mess.sender]+1))
	    						{
	    							vClockForSequencer[mess.sender]+=1;	
	    							MessageBody m=new MessageBody(mess.messageId, "order", "" , AVDNumber, vClockForSequencer);
	    							m.setSequencenumber(globalSqNumber++);
	    							new ClientThread(m,ports).start();
	    							it.remove();
	    						}  // end of if 254
	    					} //end of while 246
	    				} // end of if 244
	
	        		}
	            	catch (Exception e) {
	    				// TODO: handle exception 
	            		Log.e(TAG, e.getMessage());
	    			}
	        	}

        	}
        	

        	
        	//Implementation of the total ordering  	
        	
    		while (deliverQueue.size() != 0) {
    			MessageBody front = deliverQueue.peek();
    			if(front.sequenceNumber==localSqNumber && waitingQ.containsKey(front.messageId))
    			{
    				
    				localSqNumber++;
    				final String messageStr=front.message;
    				waitingQ.remove(front.messageId);	
    				deliverQueue.poll();

    				ContentValues  keyValueToInsert = new ContentValues();
    				// inserting <”key-to-insert”, “value-to-insert”>
    				keyValueToInsert.put("key", String.valueOf(front.sequenceNumber));
    				keyValueToInsert.put("value", messageStr);
    				Uri providerUri= Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger.provider");
    				
    				
    				Uri newUri = getContentResolver().insert(
    				    providerUri,    // assume we already created a Uri object with our provider URI
    				    keyValueToInsert
    				);
    			
                	runOnUiThread(new Runnable() {
    					
    					@Override
    					public void run() {
    						// TODO Auto-generated method stub
    						onProgressUpdate(messageStr);
    					}
    				});
    				//onProgressUpdate(messageStr);								
    			}
    			else 
    				break;
    			
    		}
        	
        }
    	
    }

    public class MyComparator implements Comparator<MessageBody>
    {
    	public int compare(MessageBody a,MessageBody b)
    	{
    		return (a.sequenceNumber-b.sequenceNumber);
    	}
    }
       
}



