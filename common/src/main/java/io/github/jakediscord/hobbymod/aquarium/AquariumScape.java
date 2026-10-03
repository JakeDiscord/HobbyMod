package io.github.jakediscord.hobbymod.aquarium;

import java.util.*;
/** Bounded, item-backed aquascaping positions in normalized tank coordinates. */
public final class AquariumScape {
    public enum Material { SEAGRASS,KELP,ROCK,WOOD }
    public record Piece(Material material,double x,double z,int rotation){}
    private final List<Piece> pieces=new ArrayList<>();
    public List<Piece> pieces(){return List.copyOf(pieces);}
    public boolean add(Material material,double x,double z,int rotation){
        if(material==null || !Double.isFinite(x) || !Double.isFinite(z) || x<.08 || x>.92 || z<.08 || z>.92 || pieces.size()>=28)return false;
        long plants=pieces.stream().filter(p->p.material==Material.SEAGRASS || p.material==Material.KELP).count();
        long count=pieces.stream().filter(p->p.material==material).count();
        if((material==Material.SEAGRASS || material==Material.KELP)?plants>=16:count>=(material==Material.ROCK?8:4))return false;
        pieces.add(new Piece(material,x,z,Math.floorMod(rotation,4)));return true;
    }
    public Piece remove(int index){return index>=0 && index<pieces.size()?pieces.remove(index):null;}
    public void clear(){pieces.clear();}
    public int count(Material m){return (int)pieces.stream().filter(p->p.material==m).count();}
}
