package br.com.kuntzedevprojects.money_master_2.exceptions;

public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = -105272490824369734L;

	public BusinessException(String message) {
        super(message);
    }
}
