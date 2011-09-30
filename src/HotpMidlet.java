import com.google.authenticator.blackberry.PasscodeGenerator;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import util.HexBin;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import java.util.Calendar;

public class HotpMidlet extends MIDlet {

    private final static Command CMD_EXIT = new Command("Exit", Command.EXIT, 1);

    public void startApp() {
        theDisplay = Display.getDisplay(this);

        theForm = new Form("HOTP/T30");
        theForm.addCommand(CMD_EXIT);
        theForm.setCommandListener(new CommandListener() {
            public void commandAction(Command command, Displayable displayable) {
                destroyApp(false);
                notifyDestroyed();
            }
        });

        final String SECRET = getAppProperty("MIDlet-Vendor").trim();

        theGauge = new Gauge("", false, 30, getProgressValue());
        thePinLabel = new StringItem("      ", computePin(SECRET, null));
        thePinLabel.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));

        theForm.append(theGauge);
        theForm.append(new Spacer(100, 20));
        theForm.append(thePinLabel);

        theDisplay.setCurrent(theForm);


        new Thread() {
            public void run() {

                while (theIsRunning) {
                    thePinLabel.setText(computePin(SECRET, null));


                    theGauge.setValue(getProgressValue());

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        thePinLabel.setText(e.getMessage());
                    }
                }
            }
        }.start();
    }

    private int getProgressValue() {
        Calendar cal = Calendar.getInstance();
        int seconds = cal.get(Calendar.SECOND);
        return seconds % 30; // < 30 ? seconds : 60 - seconds;
    }


    public static String computePin(String secret, Long counter) {
        try {
            final byte[] keyBytes = HexBin.decode(secret); //Base32String.decode(secret);
            Mac mac = new HMac(new SHA1Digest());
            mac.init(new KeyParameter(keyBytes));
            PasscodeGenerator pcg = new PasscodeGenerator(mac);

            String pin;
            if (counter == null) { // time-based totp
                pin =  pcg.generateTimeoutCode();
            } else { // counter-based hotp
                pin = pcg.generateResponseCode(counter.longValue());
            }
            String formattedPin = pin.substring(0, 3) + " "+pin.substring(3);
            return formattedPin;
        } catch (RuntimeException e) {
            return "General security exception: " + e.getMessage();
        }
    }

    /**
     * Pause is a no-op since there are no background activities or
     * record stores that need to be closed.
     */
    public void pauseApp() {
        theIsRunning = false;
    }

    /**
     * Destroy must cleanup everything not handled by the garbage collector.
     * In this case there is nothing to cleanup.
     */
    public void destroyApp(boolean unconditional) {
        theIsRunning = false;
    }

    private boolean theIsRunning = true;
    private Display theDisplay;
    private Form theForm;
    private StringItem thePinLabel;
    private Gauge theGauge;


}