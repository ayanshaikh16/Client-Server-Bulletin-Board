public class ProtocolException extends Exception{
    private final ErrorCode code;

    public ProtocolException(ErrorCode code, String message){
        super(message);
        this.code = code;
    }

    public ErrorCode getCode(){
        return code;
    }
}
