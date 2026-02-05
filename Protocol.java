public class Protocol{
    public static String ok(){
        return "OK";
    }

    public static String error(ErrorCode code, String message){
        message = message.replace('\n', ' ').replace('\r', ' ');
        return "ERROR " + code.name() + " " + message;
    }

}