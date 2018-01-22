

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import net.sf.json.*;


@ServerEndpoint(value = "/websocket/chat")
public class ChatAnnotation {

  
    private static final String GUEST_PREFIX = "Guest";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<ChatAnnotation> connections =
            new CopyOnWriteArraySet<ChatAnnotation>();
    private static final ArrayList<String> userList=new ArrayList<String>();
    private final String nickname;
    private Session session;

    public ChatAnnotation() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    }


    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        System.out.println(nickname+" joins");
        String message = String.format("* %s %s", nickname, "has joined.");
        addUserList();
        System.out.println(getUserList());
        broadcast(getUserList());
        broadcast(message);

        //broadcast(getUserList());
    }


    @OnClose
    public void end() {
        connections.remove(this);
        String message = String.format("* %s %s",
                nickname, "has disconnected.");
        System.out.println(nickname+" disconnects");
        subUserList(nickname);
        System.out.println(getUserList());
        broadcast(message);
    }


    @OnMessage
    public void incoming(String message) {
        // Never trust the client
        String filteredMessage = String.format("%s: %s",
                nickname, HTMLFilter.filter(message.toString()));
        broadcast(filteredMessage);
    }




    @OnError
    public void onError(Throwable t) throws Throwable {
      
    }


    private static void broadcast(String msg) {
        for (ChatAnnotation client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
              
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                String message = String.format("* %s %s",
                        client.nickname, "has been disconnected.");
                broadcast(message);
            }
        }
    }
    private static void addUserList() {
    	boolean found;
    	for (ChatAnnotation client : connections) {
            try {
            	found=false;
                synchronized (client) {
                    client.session.getBasicRemote().sendText("");
                    try {
	                    for(String userName:userList) {
	                    	if(userName.equals(client.nickname)) {
	                    		found=true;
	                    		break;//找到就跳出循环，设置found为true
	                    	}
	                    }
                    }
                    catch(NullPointerException e) {
                    	e.printStackTrace();//第一次userList为空
                    }
                    if(!found)
                    	userList.add(client.nickname);
                }
            } 
            catch (IOException e) {
	        	connections.remove(client);
	            try {
	                client.session.close();
	            } catch (IOException e1) {
	                // Ignore
	            }
            }
        }
    	
    }
    
    private static void subUserList(String nickname) {
    	for(String userName:userList) {
    		if(userName.equals(nickname)) {
    			userList.remove(userName);
    		}
    	}
    }
    
    private static String getUserList() {
    	return userList.toString();
    }
    
   
}
