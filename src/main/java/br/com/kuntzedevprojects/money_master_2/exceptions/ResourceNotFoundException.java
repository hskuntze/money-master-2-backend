package br.com.kuntzedevprojects.money_master_2.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1922122895548810970L;

	public ResourceNotFoundException(String message) {
        super(message);
    }
}
