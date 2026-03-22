package org.ada.com;

import org.ada.com.adapters.in.cli.TerminalApp;
import org.ada.com.config.AppFactory;

public class Main {
    public static void main(String[] args) {
        TerminalApp terminalApp = AppFactory.createTerminalApp();
        terminalApp.run();
    }
}
