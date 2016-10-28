package edu.ouc.principle.netty;

public class ZkClient {
	
	private volatile State state = State.CONNECTED;
	
	
	enum State{
		//��������
		CONNECTING,
		//������
		CONNECTED,
		//�ѹر�
		CLOSE;
		
		public boolean isConnected(){
			return this == CONNECTED;
		}
		public boolean isClosed(){
			return this == CLOSE;
		}
	}
}
