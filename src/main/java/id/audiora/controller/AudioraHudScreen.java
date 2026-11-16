package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AudioraHudScreen extends Screen {
    private final Screen parent;
    private int selected;
    private int scroll;
    private boolean panelRight = true;

    public AudioraHudScreen(Screen parent) {
        super(Component.literal("Audiora HUD Settings"));
        this.parent=parent;
    }

    @Override protected void init() {
        clearWidgets();
        int panelX = panelRight ? Math.max(8,width-174) : 8;
        int y=34-scroll;
        this.addRenderableWidget(Button.builder(Component.literal(panelRight?"Panel: RIGHT →":"← Panel: LEFT"), b->{
            panelRight=!panelRight; rebuild();
        }).bounds(panelX,y,164,20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Selected: "+hudName()),b->{
            selected=(selected+1)%AudioraConfig.HUD_IDS.length; rebuild();
        }).bounds(panelX,y+26,164,20).build());

        this.addRenderableWidget(Button.builder(toggleLabel(),b->{
            if(selected==7) AudioraConfig.comboEnabled=!AudioraConfig.comboEnabled; AudioraConfig.hudEnabled[7]=AudioraConfig.comboEnabled;
            else AudioraConfig.hudEnabled[selected]=!AudioraConfig.hudEnabled[selected];
            b.setMessage(toggleLabel()); AudioraConfig.save();
        }).bounds(panelX,y+52,164,20).build());

        this.addRenderableWidget(Button.builder(Component.literal("←"),b->{AudioraConfig.hudX[selected]-=5;AudioraConfig.save();})
            .bounds(panelX,y+80,38,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("→"),b->{AudioraConfig.hudX[selected]+=5;AudioraConfig.save();})
            .bounds(panelX+42,y+80,38,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("↑"),b->{AudioraConfig.hudY[selected]-=5;AudioraConfig.save();})
            .bounds(panelX+84,y+80,38,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("↓"),b->{AudioraConfig.hudY[selected]+=5;AudioraConfig.save();})
            .bounds(panelX+126,y+80,38,20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Size −"),b->{
            AudioraConfig.hudScale[selected]=Math.max(50,AudioraConfig.hudScale[selected]-10);AudioraConfig.save();
        }).bounds(panelX,y+108,78,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Size +"),b->{
            AudioraConfig.hudScale[selected]=Math.min(200,AudioraConfig.hudScale[selected]+10);AudioraConfig.save();
        }).bounds(panelX+84,y+108,80,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset selected HUD"),b->{
            AudioraConfig.hudX[selected]=0;AudioraConfig.hudY[selected]=0;AudioraConfig.hudScale[selected]=100;
            if(selected==7){AudioraConfig.comboEnabled=true;AudioraConfig.hudEnabled[7]=true;}else AudioraConfig.hudEnabled[selected]=true;
            AudioraConfig.save();rebuild();
        }).bounds(panelX,y+136,164,20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose())
            .bounds(panelX,y+166,164,20).build());

        this.addRenderableWidget(Button.builder(Component.literal("◀ Previous"),b->{
            selected=(selected+AudioraConfig.HUD_IDS.length-1)%AudioraConfig.HUD_IDS.length;rebuild();
        }).bounds(width/2-94,height-32,90,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Next ▶"),b->{
            selected=(selected+1)%AudioraConfig.HUD_IDS.length;rebuild();
        }).bounds(width/2+4,height-32,90,20).build());
    }

    private void rebuild(){clearWidgets();init();}
    private String hudName(){
        return switch(AudioraConfig.HUD_IDS[selected]){
            case "target" -> "Mob name / health";
            case "player_target" -> "Player name / health";
            case "critical_hit" -> "Critical hit text";
            case "combo" -> "Crit combo HUD";
            default -> AudioraConfig.HUD_IDS[selected].replace('_',' ');
        };
    }
    private Component toggleLabel(){
        boolean on=selected==7?AudioraConfig.comboEnabled:AudioraConfig.hudEnabled[selected];
        return Component.literal("HUD: "+(on?"ON":"OFF"));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);
        int x=panelRight?Math.max(8,width-174):8;
        g.fill(x-4,24,x+168,222,0xAA101A2B);
        g.text(this.font,"AUDIORA • HUD SETTINGS",x,12,0xFFFFFFFF,true);
        g.text(this.font,"Selected: "+hudName(),x,246,0xFFFFD45A,true);
        g.text(this.font,"Drag HUD positions in-game via offsets.",x,260,0xFFB8D8FF,false);
        g.text(this.font,"All HUD toggles are managed here, including mob names and Crit.",x,274,0xFFB8D8FF,false);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int x=panelRight?Math.max(8,width-174):8;
        g.fill(x-4,24,x+168,222,0xAA101A2B);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){AudioraConfig.save();this.minecraft.gui.setScreen(parent);}
}
