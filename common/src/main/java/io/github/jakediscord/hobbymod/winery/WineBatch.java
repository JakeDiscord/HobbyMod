package io.github.jakediscord.hobbymod.winery;

import java.io.*;
import java.util.*;

/** Compact gameplay model; one batch makes four bottles. No Minecraft or loader dependencies. */
public final class WineBatch {
    public static final int FRUIT_PER_BATCH=8,BOTTLES=4,FERMENT_TICKS=1200,MAX_BYTES=256;
    private static final long MAX_TIME=24000L*365;
    public enum Stage { MUST,FERMENTING,READY_TO_RACK,AGING,BOTTLED }
    public enum Kind { RED,WHITE,ROSE;
        public String label(){return switch(this){case RED->"Red";case WHITE->"White";case ROSE->"Rosé";};}
    }
    public record Fruit(boolean red,int ripeness,boolean sunlit){public Fruit{if(ripeness<3 || ripeness>6)throw new IllegalArgumentException("Fruit isn't harvestable");}}
    public UUID id=UUID.randomUUID();
    public String label="";
    public int vintage,red,sugar,acidity,tannin,baseQuality,remaining=BOTTLES;
    public Stage stage=Stage.MUST;
    public long fermented,rested,aged,darkAged,badFerment,lastClock=-1;
    public boolean cloudy;
    public Kind kind(){return red>=75?Kind.RED:red<=25?Kind.WHITE:Kind.ROSE;}
    public long idealAge(){return kind()==Kind.RED?2400:kind()==Kind.WHITE?1200:1800;}
    public int fermentation(){return (int)(fermented*100/FERMENT_TICKS);}
    public int oak(){return (int)Math.min(100,aged*100/4800);}
    public int quality(){
        int q=baseQuality-(cloudy?12:0)-(int)(badFerment*18/FERMENT_TICKS)-(int)Math.min(8,rested/2400);
        if(stage==Stage.AGING || stage==Stage.BOTTLED){double maturity=Math.min(1,aged/(double)idealAge());q+=(int)(maturity*10);q-=(int)Math.min(24,Math.max(0,aged-idealAge()*3)/1200);q-=(int)Math.min(10,Math.max(0,aged-darkAged)/1200);}
        return Math.clamp(q,10,100);
    }
    public String ageLabel(){return aged<idealAge()?"Young":aged<=idealAge()*3?"Balanced":"Past its peak";}
    public String tastingNotes(){String notes=kind()==Kind.WHITE?"Citrus and orchard fruit":kind()==Kind.ROSE?"Fresh berries and blossom":"Dark berries and gentle tannins";if(sugar>=80)notes+=" · sweet finish";else if(acidity>=60)notes+=" · bright acidity";if(oak()>=25)notes+=" · oak";if(cloudy)notes+=" · cloudy";if(aged>idealAge()*3)notes+=" · tired finish";return notes;}
    public static WineBatch press(List<Fruit> fruit,int vintage){
        if(fruit.size()!=FRUIT_PER_BATCH)throw new IllegalArgumentException("Eight grapes required");var b=new WineBatch();b.vintage=Math.max(0,vintage);
        int r=0,s=0,a=0,q=0;for(var f:fruit){r+=f.red?1:0;s+=switch(f.ripeness){case 3->52;case 4->72;case 5->86;default->92;};a+=switch(f.ripeness){case 3->72;case 4->45;case 5->32;default->24;};q+=(switch(f.ripeness){case 3->60;case 4->82;case 5->85;default->56;})+(f.sunlit?5:0);}
        b.red=r*100/FRUIT_PER_BATCH;b.sugar=s/FRUIT_PER_BATCH;b.acidity=a/FRUIT_PER_BATCH;b.tannin=8+b.red*52/100;b.baseQuality=q/FRUIT_PER_BATCH;return b;
    }
    public boolean start(long now,boolean clean){if(stage!=Stage.MUST)return false;cloudy=!clean;stage=Stage.FERMENTING;lastClock=Math.max(0,now);return true;}
    /** Advance by elapsed loaded-world time, including unloaded chunks and restarts. A rollback cannot double-count time. */
    public void advance(long now,boolean hot,boolean dark){
        now=Math.max(0,now);if(lastClock<0){lastClock=now;return;}if(now<lastClock)return;long elapsed=Math.min(MAX_TIME,now-lastClock);lastClock=now;
        if(stage==Stage.FERMENTING){long n=Math.min(elapsed,FERMENT_TICKS-fermented);fermented+=n;if(hot)badFerment+=n;elapsed-=n;if(fermented>=FERMENT_TICKS)stage=Stage.READY_TO_RACK;}
        if(stage==Stage.READY_TO_RACK)rested=Math.min(MAX_TIME,rested+elapsed);
        if(stage==Stage.AGING){aged=Math.min(MAX_TIME,aged+elapsed);if(dark && !hot)darkAged=Math.min(aged,darkAged+elapsed);}
    }
    public boolean rack(long now){if(stage!=Stage.READY_TO_RACK)return false;stage=Stage.AGING;lastClock=Math.max(0,now);return true;}
    public WineBatch bottled(String name){var b=copy();b.label=clean(name);if(b.label.isBlank())b.label=b.kind().label()+" vineyard wine";b.stage=Stage.BOTTLED;return b;}
    public WineBatch copy(){try{return decode(encode());}catch(IOException e){throw new IllegalStateException(e);}}
    public byte[] encode(){try{var bytes=new ByteArrayOutputStream();var o=new DataOutputStream(bytes);o.writeInt(0x48574931);o.writeLong(id.getMostSignificantBits());o.writeLong(id.getLeastSignificantBits());o.writeUTF(clean(label));o.writeInt(vintage);for(int v:new int[]{red,sugar,acidity,tannin,baseQuality,remaining,stage.ordinal()})o.writeByte(v);for(long v:new long[]{fermented,rested,aged,darkAged,badFerment,lastClock})o.writeLong(v);o.writeBoolean(cloudy);return bytes.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    public static WineBatch decode(byte[] data)throws IOException{
        if(data.length>MAX_BYTES)throw new IOException("Batch too large");try{var in=new DataInputStream(new ByteArrayInputStream(data));if(in.readInt()!=0x48574931)throw new IOException("Unknown vintage format");var b=new WineBatch();b.id=new UUID(in.readLong(),in.readLong());b.label=in.readUTF();if(!b.label.equals(clean(b.label)))throw new IOException("Invalid label");b.vintage=in.readInt();if(b.vintage<0)throw new IOException("Invalid vintage");b.red=range(in.readUnsignedByte(),100);b.sugar=range(in.readUnsignedByte(),100);b.acidity=range(in.readUnsignedByte(),100);b.tannin=range(in.readUnsignedByte(),100);b.baseQuality=range(in.readUnsignedByte(),100);b.remaining=range(in.readUnsignedByte(),BOTTLES);b.stage=Stage.values()[range(in.readUnsignedByte(),Stage.values().length-1)];b.fermented=time(in.readLong(),FERMENT_TICKS);b.rested=time(in.readLong(),MAX_TIME);b.aged=time(in.readLong(),MAX_TIME);b.darkAged=time(in.readLong(),b.aged);b.badFerment=time(in.readLong(),b.fermented);b.lastClock=in.readLong();if(b.lastClock< -1)throw new IOException("Invalid clock");b.cloudy=in.readBoolean();if(in.available()!=0)throw new IOException("Trailing vintage data");return b;}catch(IllegalArgumentException e){throw new IOException("Invalid batch",e);}
    }
    private static int range(int n,int max)throws IOException{if(n>max)throw new IOException("Invalid batch value");return n;}
    private static long time(long n,long max)throws IOException{if(n<0 || n>max)throw new IOException("Invalid batch time");return n;}
    public static String clean(String s){if(s==null)return "";s=s.replaceAll("[\\p{Cntrl}§]","");return s.substring(0,Math.min(32,s.length()));}
}
