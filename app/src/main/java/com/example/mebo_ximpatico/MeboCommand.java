package com.example.mebo_ximpatico;

public class MeboCommand {

    int messageCount = 0;

    String new_cmd() {
        String result = "!" + this._to_base64(this.messageCount & 63);
        this.messageCount += 1;
        return result;
    }


    String _to_base64(int val) {
        char [] str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
        return String.valueOf(str[val & 63]);
    }

    String _encode_base64(int val, int chars_count) {
        String result = "";
        for(int i = 0; i < chars_count; i++)
            result += this._to_base64(val >> (i * 6));
        return result;
    }

    String enc_spd(int speed) {
        return this._encode_base64(speed, 2);
    }

    String _command_string(CMD cmd, int para) {

        if (cmd == CMD.READERS)                     return "READERS=?";
        else if ( cmd == CMD.FACTORY)               return "P";
        else if ( cmd == CMD.BAT)                   return "BAT=?";

        else if ( cmd == CMD.LIGHT_ON)              return this.new_cmd() + "RAAAAAAAVd";
        else if ( cmd == CMD.LIGHT_OFF)             return this.new_cmd() + "RAAAAAAAVc";

        else if ( cmd == CMD.WHEEL_LEFT_FORWARD)    return this.new_cmd() + "F" + this.enc_spd(para);
        else if ( cmd == CMD.WHEEL_LEFT_BACKWARD)   return this.new_cmd() + "F" + this.enc_spd(-para);
        else if ( cmd == CMD.WHEEL_RIGHT_FORWARD)   return this.new_cmd() + "E" + this.enc_spd(para);
        else if ( cmd == CMD.WHEEL_RIGHT_BACKWARD)  return this.new_cmd() + "E" + this.enc_spd(-para);
        else if ( cmd == CMD.WHEEL_BOTH_STOP)       return this.new_cmd() + "B";

        else if ( cmd == CMD.ARM_UP)                return this.new_cmd() + "G" + this.enc_spd(para);
        else if ( cmd == CMD.ARM_DOWN)              return this.new_cmd() + "G" + this.enc_spd(-para);
        else if ( cmd == CMD.ARM_POSITION)          return this.new_cmd() + "K" + this.enc_spd(para);
        else if ( cmd == CMD.ARM_STOP)              return this.new_cmd() + "CEAA";
        else if ( cmd == CMD.ARM_QUERY)             return "ARM=?";

        else if ( cmd == CMD.WRIST_UD_UP)           return this.new_cmd() + "H" + this.enc_spd(para);
        else if ( cmd == CMD.WRIST_UD_DOWN)         return this.new_cmd() + "H" + this.enc_spd(-para);
        else if ( cmd == CMD.WRIST_UD_POSITION)     return this.new_cmd() + "L" + this.enc_spd(para);
        else if ( cmd == CMD.WRIST_UD_STOP)         return this.new_cmd() + "CIAA";
        else if ( cmd == CMD.WRIST_UD_QUERY)        return "WRIST_UD=?";

        else if ( cmd == CMD.WRIST_ROTATE_LEFT)     return this.new_cmd() + "I" + this.enc_spd(para);
        else if ( cmd == CMD.WRIST_ROTATE_RIGHT)    return this.new_cmd() + "I" + this.enc_spd(-para);
        else if ( cmd == CMD.WRIST_ROTATE_POSITION) return this.new_cmd() + "M" + this.enc_spd(para);
        else if ( cmd == CMD.WRIST_ROTATE_STOP)     return this.new_cmd() + "CQAA";
        else if ( cmd == CMD.WRIST_ROTATE_QUERY)    return "WRIST_ROTATE=?";

        else if ( cmd == CMD.CLAW_POSITION)         return this.new_cmd() + "N" + this.enc_spd(para);
        else if ( cmd == CMD.CLAW_STOP)             return this.new_cmd() + "CgAA";
        else if ( cmd == CMD.CLAW_QUERY)            return "CLAW=?";

        else if ( cmd == CMD.CAL_ARM)               return this.new_cmd() + "DE";
        else if ( cmd == CMD.CAL_WRIST_UD)          return this.new_cmd() + "DI";
        else if ( cmd == CMD.CAL_WRIST_ROTATE)      return this.new_cmd() + "DQ";
        else if ( cmd == CMD.CAL_CLAW)              return this.new_cmd() + "Dg";
        else if ( cmd == CMD.CAL_ALL)               return this.new_cmd() + "D_";

        else if ( cmd == CMD.VERSION_QUERY)         return "VER=?";
        else if ( cmd == CMD.REBOOT_CMD)            return this.new_cmd() + "DE";
        else if ( cmd == CMD.JOINT_SPEED)           return "";

        else if ( cmd == CMD.SET_REG)               return "";
        else if ( cmd == CMD.QUERY_REG)             return "REG" + (para / 100 % 10) + (para / 10 % 10) + (para % 10) + "=?";
        else if ( cmd == CMD.SAVE_REG)              return "REG=FLUSH";

        else if ( cmd == CMD.WHEEL_LEFT_SPEED)      return this.new_cmd() + "F" + this.enc_spd(para);
        else if ( cmd == CMD.WHEEL_RIGHT_SPEED)     return this.new_cmd() + "E" + this.enc_spd(para);

        else if ( cmd == CMD.QUERY_EVENT)           return "*";
        else                                            return "";
    }


    String generate_single_command(int number, CMD command, int parameter) {
        String cmd_str = this._command_string(command, parameter);
        if(command == CMD.EYE_LED_STATE)
            return "command" + number + "=eye_led_state()";
        if(command == CMD.CLAW_LED_STATE)
            return "command" + number + "=claw_led_state()";
        if(command == CMD.GET_SSID)
            return "command" + number + "=get_ssid()";
        if(command == CMD.VIDEO_FLIP)
            return "command" + number + "=video_flip(0)";
        if(command == CMD.VIDEO_MIRROR)
            return "command" + number + "=video_mirror(0)";
        if(command == CMD.ACEAA)
            return "command" + number + "=mebolink_message_send(!ACEAA)";
        if(command == CMD.BCQAA)
            return "command" + number + "=mebolink_message_send(!BCQAA)";
        if(command == CMD.CCIAA)
            return "command" + number + "=mebolink_message_send(!CCIAA)";
        if(command == CMD.INIT_ALL)
            return "command" + number + "=mebolink_message_send(!CVVDSAAAAAAAAAAAAAAAAAAAAAAAAYtBQfA4uAAAAAAAAAAQfAoPAcXAAAA)";
        return "command" + number + "=mebolink_message_send(" + cmd_str + ")";

    }
    public static enum CMD {
        READERS,
        FACTORY,
        BAT,

        WHEEL_LEFT_FORWARD,
        WHEEL_LEFT_BACKWARD,
        WHEEL_RIGHT_FORWARD,
        WHEEL_RIGHT_BACKWARD,
        WHEEL_BOTH_STOP,

        ARM_UP,
        ARM_DOWN,
        ARM_POSITION,
        ARM_STOP,
        ARM_QUERY,

        WRIST_UD_UP,
        WRIST_UD_DOWN,
        WRIST_UD_POSITION,
        WRIST_UD_STOP,
        WRIST_UD_QUERY,

        WRIST_ROTATE_LEFT,
        WRIST_ROTATE_RIGHT,
        WRIST_ROTATE_POSITION,
        WRIST_ROTATE_STOP,
        WRIST_ROTATE_QUERY,

        CLAW_POSITION,
        CLAW_STOP,
        CLAW_QUERY,

        SET_TURNING_SPEED_1,
        SET_TURNING_SPEED_2,
        SET_TURNING_SPEED_3,

        CAL_ARM,
        CAL_WRIST_UD,
        CAL_WRIST_ROTATE,
        CAL_CLAW,
        CAL_ALL,

        VERSION_QUERY,
        REBOOT_CMD,
        JOINT_SPEED,

        SET_REG,
        QUERY_REG,
        SAVE_REG,

        WHEEL_LEFT_SPEED,
        WHEEL_RIGHT_SPEED,

        QUERY_EVENT,

        LIGHT_ON,
        LIGHT_OFF,
        EYE_LED_STATE,
        CLAW_LED_STATE,
        GET_SSID,
        VIDEO_FLIP,
        VIDEO_MIRROR,
        ACEAA,
        BCQAA,
        CCIAA,
        INIT_ALL,
    }
}
