package edu.ouc.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server Processor
 * 
 * @author wqx
 */
public class Processor implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

	Reactor reactor;

	private SocketChannel sc;

	private final SelectionKey sk;

	private final ByteBuffer lenBuffer = ByteBuffer.allocate(4);

	private ByteBuffer inputBuffer = lenBuffer;

	private ByteBuffer outputDirectBuffer = ByteBuffer.allocateDirect(1024 * 64);

	private LinkedBlockingQueue<ByteBuffer> outputQueue = new LinkedBlockingQueue<ByteBuffer>();

	public Processor(Reactor reactor, Selector sel,SocketChannel channel) throws IOException{
		this.reactor = reactor;
		sc = channel;
		sc.configureBlocking(false);
		sk = sc.register(sel, SelectionKey.OP_READ);
		sk.attach(this);
		sel.wakeup();
	}

	@Override
	public void run() {
		if(sc.isOpen() && sk.isValid()){
			if(sk.isReadable()){
				doRead();
			}else if(sk.isWritable()){
				doSend();
			}
		}else{
			LOG.error("try to do read/write operation on null socket");
			try {
				if(sc != null)
					sc.close();
			} catch (IOException e) {}
		}
	}
	private void doRead(){
		try {
			int byteSize = sc.read(inputBuffer);
			
			if(byteSize < 0){
				LOG.error("Unable to read additional data");
			}
			if(!inputBuffer.hasRemaining()){
				
				if(inputBuffer == lenBuffer){
					//read length
					inputBuffer.flip();
					int len = inputBuffer.getInt();
					if(len < 0){
						throw new IllegalArgumentException("Illegal data length");
					}
					//prepare for receiving data
					inputBuffer = ByteBuffer.allocate(len);
				}else{
					//read data
					if(inputBuffer.hasRemaining()){
						sc.read(inputBuffer);
					}
					if(!inputBuffer.hasRemaining()){
						inputBuffer.flip();
						processRequest();
						//clear lenBuffer and waiting for next reading operation 
						lenBuffer.clear();
						inputBuffer = lenBuffer;
					}
				}
			}
		} catch (IOException e) {
			LOG.error("Unexcepted Exception during read. e=" + e);
			try {
				if(sc != null)
					sc.close();
			} catch (IOException e1) {
				LOG.warn("Ignoring exception when close socketChannel");
			}
		}
	}

	/**
	 * process request and get response
	 * 
	 * @param request
	 * @return
	 */
	private void processRequest(){
		reactor.processRequest(this,inputBuffer);
	}
	private void doSend(){
		try{
			/**
			 * write data to channel��
			 * step 1: write the length of data(occupy 4 byte)
			 * step 2: data content
			 */
			if(outputQueue.size() > 0){
				ByteBuffer directBuffer = outputDirectBuffer;
				directBuffer.clear();
				
				for(ByteBuffer buf : outputQueue){
					buf.flip();
					
					if(buf.remaining() > directBuffer.remaining()){
						//prevent BufferOverflowException
						buf = (ByteBuffer) buf.slice().limit(directBuffer.remaining());
					}
					//transfers the bytes remaining in buf into  directBuffer
					int p = buf.position();
					directBuffer.put(buf);
					//reset position
					buf.position(p);

					if(!directBuffer.hasRemaining()){
						break;
					}
				}
				directBuffer.flip();
				int sendSize = sc.write(directBuffer);
				
				while(!outputQueue.isEmpty()){
					ByteBuffer buf = outputQueue.peek();
					int left = buf.remaining() - sendSize;
					if(left > 0){
						buf.position(buf.position() + sendSize);
						break;
					}
					sendSize -= buf.remaining();
					outputQueue.remove();
				}
			}
			synchronized(reactor){
				if(outputQueue.size() == 0){
					//disable write
					disableWrite();
				}else{
					//enable write
					enableWrite();
				}
			}
		} catch (CancelledKeyException e) {
            LOG.warn("CancelledKeyException occur e=" + e);
        } catch (IOException e) {
            LOG.warn("Exception causing close, due to " + e);
        }
	}
	public void sendBuffer(ByteBuffer bb){
		try{
			synchronized(this.reactor){
				if(LOG.isDebugEnabled()){
					LOG.debug("add sendable bytebuffer into outputQueue");
				}
				//wrap ByteBuffer with length header
				ByteBuffer wrapped = wrap(bb);
				
				outputQueue.add(wrapped);
				
				enableWrite();
			}
		}catch(Exception e){
			LOG.error("Unexcepted Exception: ", e);
		}
	}
	
	private ByteBuffer wrap(ByteBuffer bb){
		bb.flip();
		lenBuffer.clear();
		int len = bb.remaining();
		lenBuffer.putInt(len);
		ByteBuffer resp = ByteBuffer.allocate(len+4);
		lenBuffer.flip();
		
		resp.put(lenBuffer);
		resp.put(bb);
		return resp;
	}
	private void enableWrite(){
		int i = sk.interestOps();
		if((i & SelectionKey.OP_WRITE) == 0){
			sk.interestOps(i | SelectionKey.OP_WRITE);
		}
	}
	private void disableWrite(){
		int i = sk.interestOps();
		if((i & SelectionKey.OP_WRITE) == 1){
			sk.interestOps(i & (~SelectionKey.OP_WRITE));			
		}
	}
}
