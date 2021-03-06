import org.apache.commons.cli.*;

import javax.mail.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static String appName = "malooer";
    public static final int DEFAULT_INTERVAL = 300;

    /**
     * Required parameters:
     * -port <PORT_NUMBER>
     * -host <HOST>
     * -user <USER>
     * -pwd <PASSWORD>
     *
     * optional parameters:
     * -i <INTERVAL>
     * -o <EXECUTE_ONCE>
     *
     * @param args
     */
    public static void main(String[] args) {
        CommandLine cl;

        if (args.length < 1) {
            printHelp();
            return;
        }

        try {
            cl = getCommandline(args);
        } catch (ParseException e) {
            printHelp();
            return;
        }

        if (!validateCmdLine(cl)) {
            printHelp();
            return;
        }

        final Integer port = Integer.valueOf(cl.getOptionValue("port"));
        final String host = cl.getOptionValue("host");
        final String user = cl.getOptionValue("user");
        final String pwd = cl.getOptionValue("pwd");
        boolean onlyOnce = cl.hasOption("o");

        Integer interval;
        try {
            interval = getInterval(cl);
        } catch (Exception e) {
            printHelp();
            return;
        }
        interval = interval != null ? interval : DEFAULT_INTERVAL;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(props);

        try {
            final Transport transport = session.getTransport("smtp");
            Runnable runnable = getRunnable(transport, host, port, user, pwd);
            startExecutor(runnable, onlyOnce, interval);
        } catch (NoSuchProviderException e) {
            System.out.println("No such provider");
            return;
        }
    }

    private static void printHelp() {
        HelpFormatter hFormatter = new HelpFormatter();
        hFormatter.printHelp(appName, getOptions());
    }

    private static Runnable getRunnable(final Transport transport, final String host, final Integer port, final String user, final String pwd) {
        return new Runnable() {
            public void run() {
                Date date = new Date();
                Timestamp timestamp = new Timestamp(date.getTime());
                try {
                    transport.connect(host, port, user, pwd);
                    transport.close();
                    System.out.println(timestamp + ": [OK] Connection established - " + host);
                } catch (AuthenticationFailedException e) {
                    System.out.println(timestamp + ": [FAIL] Authentication failed");
                } catch (MessagingException e) {
                    System.out.println(timestamp + ": [FAIL] Unable to connect to server");
                }
            }
        };
    }

    private static void startExecutor(Runnable runnable, boolean onlyOnce, Integer interval) {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        if (onlyOnce) {
            service.execute(runnable);
            service.shutdown();
        } else {
            service.scheduleAtFixedRate(runnable,  0, interval, TimeUnit.SECONDS);
        }
    }

    private static Integer getInterval(CommandLine cl) throws Exception {
        String interval = cl.getOptionValue("i");

        if (interval != null) {
            return Integer.valueOf(interval);
        } else {
            return null;
        }
    }

    private static boolean validateCmdLine(CommandLine cl) {
        List<Object> inputList = new ArrayList<Object>();
        inputList.add(cl.getOptionValue("port"));
        inputList.add(cl.getOptionValue("host"));
        inputList.add(cl.getOptionValue("user"));
        inputList.add(cl.getOptionValue("pwd"));

        for (Object obj : inputList) {
            if (obj == null) {
                return false;
            }
        }
        return true;
    }

    private static CommandLine getCommandline(String[] args) throws ParseException {
        Options options = getOptions();
        CommandLineParser clParser = new DefaultParser();

        return clParser.parse(options, args);
    }

    private static Options getOptions() {
        Options options = new Options();

        Option portOption = new Option("port", true, "port of server");
        Option hostOption = new Option("host", true, "the host");
        Option userOption = new Option("user", true, "the user");
        Option passOption = new Option("pwd", true, "the password");
        Option intervalOption = new Option("i", true, "interval between connections");
        Option executeOnceOption = new Option("o", false, "establish connection only once");

        options.addOption(portOption);
        options.addOption(hostOption);
        options.addOption(userOption);
        options.addOption(passOption);
        options.addOption(intervalOption);
        options.addOption(executeOnceOption);

        return options;
    }
}