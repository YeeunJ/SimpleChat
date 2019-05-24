package SimpleChat;

import java.net.*;
import java.io.*;
import java.util.*;

//server를 준비하고 sock에서 온 요청을 받고 그 sock를 chatThread에 넘겨주는 역할(chatThread에서 각 sock에서 전달하는 message를 받아서 적절하게 sock 들에게 다시 전달해준다.
public class ChatServer {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001); // 클라이언트의 요청을 받기 위한 준비(10001번 포트)->다른 서버와 연결하기 위한 것,
			System.out.println("Waiting connection...");
			HashMap<String, PrintWriter> hm = new HashMap<String, PrintWriter>();//. Key와 value를 묶어 하나의 entry로 저장, 많은양의 데이터를 검색하는데 뛰어남
			//스레드간 OutputStream을 공유하는 방법이 필요해서 해시맵 사용(id와 내용을 저장해서 
			//ChatClient가 접속 요청을 한다.
			while(true){
				Socket sock = server.accept();//서버소켓으로부터 소켓 객체 가지고 오기(연결)
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
				//새로운 쓰레드가 작업을 실행하는데 필요한 호출스택(공간)을 생성한 다음 run()을 호출해서 그 안(스택)에 run()이 저장되는 것
				//id와 내용을 받아서 일정 형식으로 바꾼 후 다른 sock에 보내주는 등의 통신과 통신을 마무리하는 것은 chatthread에서 담당함
			}
		}catch(Exception e){//예외가 생겼을때 출력해주기
			System.out.println(e);
		}
	} // main
}


//앞뒤 순서가 필요 없는 일들이 있을 때 분리해서 동시에 처리하고 싶을때 thread
//여기에서는 모든 sock에서 오는 메세지를 받아서 모든 sock나 특정 sock에게 보내주거나 sock 종료할 경우 그 sock를 삭제하는 역할
//새로운 sock가 생길때마다 생성되고 
class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap<String, PrintWriter> hm;
	private boolean initFlag = false;
	
	//constructor->새로운 sock가 생길때 pw, br을 만들어서 그 sock의 내용을 받고 hm에 저장해준다
	public ChatThread(Socket sock, HashMap<String, PrintWriter> hm){
		this.sock = sock;
		this.hm = hm;
		//sock, hm을 받아와서 초기화해주기
		
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));//출력 method ->각 sock로 메세지를 출력한다.
			br = new BufferedReader(new InputStreamReader(sock.getInputStream())); //각 sock가 보낸 메시지를 읽는다. 
			id = br.readLine();//Client로부터 받은 메세지 id에 저장
			
			broadcast(id + " entered.");//id entered 라는 내용을 sock에 보내기
			System.out.println("[Server] User (" + id + ") entered.");//server의 콘솔 창에 들어왔다고 출력
			synchronized(hm){//스레드 끼리 동기화
				hm.put(this.id, pw);//key가 sock에서 준 id고 value는 그 id를 가진 sock로 보낼 pw 값
			}
			initFlag = true;
		}catch(Exception ex){ //오류시 오류 띄우기
			System.out.println(ex);
		}
	} // constructor
	
	//전체적으로 실행해줌-> 각 명령에 따라서 적절하게 함수를 사용하여 출력해주고 sock가 나갈 경우 삭제하는 역할을 모두 총 명령을 내리는 역할
	public void run(){
		try{
			String line = null;
			//키보드를 통해 입력되는 것에 따라서 종료하거나 귓속말을 하거나 전체로 출력함
			while((line = br.readLine()) != null){
				if(prohibit_word(line))
					continue;
				if(line.equals("/quit"))
					break;
				if(line.indexOf("/userlist") == 0){
					send_userlist();
				}else if(line.indexOf("/to ") == 0){
					sendmsg(line);//sendmsg를 사용해서 특정 sock에게 전달
				}else
					broadcast(id + " : " + line);//broadcast 사용해서 모든 sock에게 전달
			}
		}catch(Exception ex){//예외사항이 생기면 에러를 출력하고
			System.out.println(ex);
		}finally{//마지막에 thread간 동기화를 한 뒤에 hm에서 id를 삭제하고
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");//broadcast method를 통해 iㅇd exit이란 메세지를 sock로 보내줌
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	
	//특정 sock에게만 msg보내는 method
	public void sendmsg(String msg){//msg는 "/to 누구에게 할말"이런 형식이므로
		int start = msg.indexOf(" ") +1;//띄어쓰기 다음 index값+1 -> 누구에게의 첫번째 인덱스 값
		int end = msg.indexOf(" ", start);//그 다음 띄어쓰기의 index값 -> 할말 바로 전의 인덱스 값
		if(end != -1){
			String to = msg.substring(start, end);//누구에게 보내는지에 대한 string 정보
			String msg2 = msg.substring(end+1);//하고 싶은 말에 대한 string 정보
			Object obj = hm.get(to);//hm에 저장된 pw를 불러와서 obj에 담기 -> 그 pw로만 정보를 전달하기 위해서!!
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;//받아온 pw로 인스턴스화 하고
				pw.println(id + " whisphered. : " + msg2);//그 pw로만 전달 준비
				pw.flush();//전달 -> 그 sock에서만 메세지를 받아서 출력해줄 수 있음
			} // if
		}
	} // sendmsg
	
	//msg를 모든 sock에서 받을 수 있도록 보내주는 method
	public void broadcast(String msg){
		Object obj = hm.get(id);
		PrintWriter pw_now = (PrintWriter)obj;
		synchronized(hm){//스레드 사이에 동기화 후에 hm안에  모든 sock에 msg보내줌
			Collection<PrintWriter> collection = hm.values(); //여러 원소들을 담을 수 있는 자료구조, 크기제한 없음-> hm에 저장한 내용을 모두 
			Iterator<PrintWriter> iter = collection.iterator(); //iterator : 콜렉션 안의 원소 하나하나에 접근하게 해주는 역할
			while(iter.hasNext()){//읽어 올 요소가 남아있는지 확인하는 메소드 있으면 true -> 읽어올 요소가 있으면 계속 반복
				PrintWriter pw = (PrintWriter)iter.next();//다음으로 넘겨주며 hm에 저장되어있었던 pw 정보를 가지고 인스턴스화 해서 출력해줌
				if(pw != pw_now) {
					pw.println(msg);//pw가 sock로 msg를 보내줌
					pw.flush();//
				}
			}
		}
	} // broadcast
	
	
	//현재까지의 사용자 id와 사용자 수 보여주는 method
	public void send_userlist(){
		synchronized(hm){
			Object obj = hm.get(id);
			PrintWriter pw = (PrintWriter)obj;
			pw.println("<userlist Info>");
			pw.flush();//전달 -> 그 sock에서만 메세지를 받아서 출력해줄 수 있음
			for(Object key : hm.keySet()) {
					pw.println(key);
					pw.flush();//전달 -> 그 sock에서만 메세지를 받아서 출력해줄 수 있음
			}
			pw.println("count : "+ hm.size());
			pw.flush();
		}
	}
	
	public boolean prohibit_word(String msg) {
		String []p_word = {"dog", "drive", "peach", "stupid", "monkey"};
		for(String s: p_word) {
			if(msg.indexOf(s) != -1) {
				Object obj = hm.get(id);
				PrintWriter pw = (PrintWriter)obj;
				pw.println("Contains banned words. You can not this message.");
				pw.flush();
				
				return true;
			}
		}
		return false;
	}
}
