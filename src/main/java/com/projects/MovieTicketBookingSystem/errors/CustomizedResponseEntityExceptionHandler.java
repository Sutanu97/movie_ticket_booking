package com.projects.MovieTicketBookingSystem.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomizedResponseEntityExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGenericExceptions(Exception ex, WebRequest webRequest){
        LOGGER.debug("In method handleGenericExceptions");
        LOGGER.error(ex.getStackTrace().toString());
        ErrorDto dto = new ErrorDto(LocalDateTime.now(), webRequest.getDescription(false), ex.getMessage() );
        if(ex.getMessage().contains("Bad credentials")){
            return new ResponseEntity<ErrorDto>(dto, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ErrorDto>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorDto> handleUserNotFoundException(Exception ex, WebRequest webRequest){
        LOGGER.debug("In method handleUserNotFoundException");
        LOGGER.error(ex.getStackTrace().toString());
        ErrorDto dto = new ErrorDto(LocalDateTime.now(), webRequest.getDescription(false), ex.getMessage());
        return new ResponseEntity<ErrorDto>(dto, HttpStatus.NO_CONTENT);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorDto> handleNumberFormatException(Exception ex, WebRequest webRequest){
        LOGGER.debug("In method handleNumberFormatException");
        LOGGER.error(ex.getStackTrace().toString());
        ErrorDto dto = new ErrorDto(LocalDateTime.now(), webRequest.getDescription(false), ex.getMessage());
        return new ResponseEntity<ErrorDto>(dto, HttpStatus.BAD_REQUEST);
    }


    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LOGGER.debug("In method handleMissingServletRequestParameter");
        LOGGER.error(ex.getStackTrace().toString());
        StringBuffer sb = new StringBuffer();
        sb.append("Missed method parameter "+ ex.getParameterName());
        ErrorDto dto = new ErrorDto(LocalDateTime.now(), request.getDescription(false), ex.getMessage());
        return new ResponseEntity<Object>(dto, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LOGGER.debug("In method handleMethodArgumentNotValid");
        LOGGER.error(ex.getStackTrace().toString());
        StringBuffer sb = new StringBuffer();
        sb.append("Method argument not valid : ");
        ex.getFieldErrors().forEach(sb::append);
        ErrorDto dto = new ErrorDto(LocalDateTime.now(), request.getDescription(false), sb.toString());
        return new ResponseEntity<Object>(dto, HttpStatus.BAD_REQUEST);
    }

}
