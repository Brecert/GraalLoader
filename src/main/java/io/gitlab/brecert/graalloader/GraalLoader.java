package io.gitlab.brecert.graalloader;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graalvm.polyglot.*;


@Plugin(
        id = "graalloader",
        name = "GraalLoader",
        description = "A loader for GraalVM",
        authors = {
                "Brecert"
        }
)
public class GraalLoader {

    @Inject
    private Logger logger;
    private Path configDirectory;
    private Path scriptDirectory;
    private Context context;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        String vmName = System.getProperty("java.vm.name");
        if (!vmName.startsWith("GraalVM")) {
            logger.error("Loaded on JVM " + vmName + " - not a GraalVM! To use GraalLoader you must load using GraalVM.");
        } else {
            try (Context context = Context.newBuilder().allowAllAccess(true).build();) {
                this.load();
                context.eval("ruby", "print('Hello from Ruby!')");
                context.eval("python", "print('Hello from Python!')");
            }
        }
    }

    public void load() {
        this.configDirectory = Sponge.getConfigManager().getPluginConfig(this).getDirectory();
        this.scriptDirectory = Paths.get(this.configDirectory.toString(), "scripts");
        this.context = Context.newBuilder()
            .allowAllAccess(true)
            .build();

        this.context.getPolyglotBindings().putMember("plugin", this);
        this.context.getPolyglotBindings().putMember("logger", this.logger);

        this.loadScripts();
    }

    public void loadScripts() {
        // TODO: Create Task to make efficient and safe
        Task.Builder taskBuilder = Task.builder();

        logger.info(Sponge.getAssetManager().toString());

        try {
            Files.walk(Paths.get(this.scriptDirectory.toString()))
                .filter(Files::isRegularFile)
                .forEach(this::loadScript);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String loadScript(Path path) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(path.toAbsolutePath().toString())));
        } catch (IOException e) {
            logger.error("Error loading script" + path.toString());
            e.printStackTrace();
        }

        Value res = this.context.eval("ruby", content);

        logger.info( "Loaded " + path.toString() );

        return res.toString();
    }

//CommandSpec rubyCommandSpec = CommandSpec.builder()
//        .description(Text.of("Run ruby!"))
//        .permission("graalloader.command.ruby")
//        .arguments(GenericArguments.remainingRawJoinedStrings((Text.of("content"))))
//        .executor(this::execute)
//        .build();
//
//    Sponge.getCommandManager().register(this, rubyCommandSpec, "rb");

//    private CommandResult execute(CommandSource src, CommandContext args) {
//        String content = args.<String>getOne("content").get();
//        Context ctx = Context.newBuilder()
//                .allowAllAccess(true)
//                .build();
//
//        ctx.getPolyglotBindings().putMember("plugin", this);
//
//        String joinedContent = "plugin = Polyglot.import('plugin') \n" +
//                content;
//
//        src.sendMessage(Text.of(ctx.eval("ruby", joinedContent)));
//
//        return CommandResult.success();
//    }
}
