module cn.jeyor1337.bozarxd {
    requires com.google.gson;
    requires commons.cli;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.base;
    requires java.desktop;
    requires java.net.http;
    requires java.scripting;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.util;
    requires static lombok;

    opens cn.jeyor1337.bozarxd.obfuscator.utils.model;
    opens cn.jeyor1337.bozarxd.ui;
}