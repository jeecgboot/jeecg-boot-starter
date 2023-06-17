package org.jeecg.chatgpt.exception;

/**
 * 
 * @author liuliu
 *
 */
public class ChatgptException extends RuntimeException{

    private static final long serialVersionUID = 6142918452640048138L;

    public ChatgptException(){
        super();
    }

    public ChatgptException(String message){
        super(message);
    }

}
