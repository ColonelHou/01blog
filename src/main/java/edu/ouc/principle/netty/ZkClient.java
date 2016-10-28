package edu.ouc.principle.netty;

public class ZkClient {
	
	private volatile State state = State.UN_CONNECTED;
	
	
	enum State{
		//��������
		CONNECTING,
		//������
		CONNECTED,
		//������
		UN_CONNECTED,
		//�ѹر�
		CLOSE;
		
		public boolean isConnected(){
			return this == CONNECTED;
		}
		public boolean isAlive(){
			return this != CLOSE;
		}
	}
}
