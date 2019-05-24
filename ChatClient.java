import java.net.*;
import java.io.*;


//처음 시작해서 socket을 만들고 서버로 접속한 뒤에 서버에 id를 넘겨주고 끝내고 싶을때 sock를 종료하는 역할
//console에 적은 내용을 server에 넘겨주는 역할도 하고 있음.
//(대신 넘겨준 뒤에 server에서 id와 함께 넘겨주는 모든 대화 내용을 출력하는 것은 inputthread에서 담당)
public class ChatClient {

	public static void main(String[] args) {
		
		if(args.length != 2){
			System.out.println("Usage : java ChatClient <username> <server-ip>");
			System.exit(1);
		}//args가 2개 들어오지 않으면 출력하고 종료
		Socket sock = null; //Socket 클래스는 client에서 서버로 접속하거나 Server에서 accept 하는데 필요한 클래스
		BufferedReader br = null; //입력된 데이터가 바로 전달되지 않고 중간에 버퍼링이 된 후에 전달
		PrintWriter pw = null;//나중에 출력해주는 역할을 함
		boolean endflag = false;
		
		try{
			sock = new Socket(args[1], 10001); //준비되어있는 ChatServer로 args[1]의 ip주소와 10001번 포트의 서버에 접속 요청을 한다. 
			pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
			//keybard로 입력받은 것을 reader가 받아들일 수 있는 형태로 바꾸어주는 인스턴스(뒤에서 사용)
			// send username.
			pw.println(args[0]);//ChatServer가 요청을 받아들였으므로 sock밖으로 사용자 id를 보낼 준비
			pw.flush(); //모두 반영(id 보냄 - 콘솔 출력이 아니라서 flush() 필요)->sever에서 받아서 hashmap에 저장한 뒤에 사용
			
			InputThread it = new InputThread(sock, br);//thread를 만들어서 현재 프로그램이 진행되는 것과 별개로 실행하도록 함
			it.start();//InputThread의 run을 실행해줌. -> 이때부터 inputThread는 이 뒤에 실행과는 별개로 실행되어서 다른 socket에서 읽어진 것을 출력해주는 역할을 함
			//새로운 쓰레드가 작업을 실행하는데 필요한 호출스택(공간)을 생성한 다음 run()을 호출해서 그 안(스택)에 run()이 저장 -> 따라서 독립적으로 작업
			String line = null;
			while((line = keyboard.readLine()) != null){//내 keyboard로 한 줄 읽어왔으면
				pw.println(line);//출력할 것을 pw를 사용해서 sock 밖으로 출력할 준비 
				//-> server에서 받아서 id와 붙여서 다시 보내줄 것, 또한 다른 sock에서 보내준 내용도 server에서 함께 보내줌
				pw.flush();//출력할 준비를 한 것을 반영->실질적으로 server에 보내줌
				if(line.equals("/quit")){//quit라고 적히면 endflag를 true로 바꾸고 연결이 끝났다고 알려주기
					endflag = true;
					break;
				}
			}
			System.out.println("Connection closed.");
		//endflag가 여전히 false라면 정상적인 종료가 아니므로 예외를 출력해준뒤 finally 수행(finally에서는 각각 pw, br, sock가 null이 아닌 경우에 pw, br, sock close)
		}catch(Exception ex){
			if(!endflag)
				System.out.println(ex);
		}finally{
			try{
				if(pw != null)
					pw.close();
			}catch(Exception ex){}
			
			try{
				if(br != null)
					br.close();
			}catch(Exception ex){}
			//sock가 비어있으면 
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		} // finally
	} // main
} // class


//앞뒤 순서가 필요 없는 일들이 있을 때 분리해서 동시에 처리하고 싶을때 thread
//여러 sock 간의 모든 통신 내용을 server에서 받아서 콘솔에 출력해주는 역할
class InputThread extends Thread{
	private Socket sock = null;
	private BufferedReader br = null;
	//constructor-> 들어온 sock와 bufferedreader로 초기화해줌
	public InputThread(Socket sock, BufferedReader br){
		this.sock = sock;
		this.br = br;
	}
	
	//thread의 run에 오버라이딩 해서 사용
	public void run(){
		try{
			String line = null;
			//버퍼링 하면서 값이 있으면 1줄 읽어와서 출력 -> server에서 
			while((line = br.readLine()) != null){//버퍼링 하면서 값이 있으면 1줄 읽어와서 출력 -> sock로 보내진 메세지를 모두 읽어와서 출력하는 역할
				System.out.println(line);
			}
		//br과 sock가 비어있지 않으면 모두 싹 출력하고 닫아줌
		}catch(Exception ex){
		}finally{
			try{
				if(br != null)
					br.close();
			}catch(Exception ex){}
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // InputThread
}
