package edu.ouc.orm;

import java.sql.Connection;

/**
 * Connection�����ӿ�
 * 
 * @author wqx
 *
 */
public interface ConnectionFactory {
	
	/**
	 * �½�Connection
	 * @return
	 */
	public Connection newConnection() throws Exception;
	
}
