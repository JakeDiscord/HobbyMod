#!/usr/bin/env python3
"""Generate original 16px winery art and Minecraft block models (Pillow required)."""
from pathlib import Path
import json, random, math
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]/'common/src/main/resources'
ASSET=ROOT/'assets/hobbymod'; DATA=ROOT/'data'
def write(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
def save(name,im,folder='block'):
 p=ASSET/f'textures/{folder}/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
def tile(name,base,kind='wood'):
 im=Image.new('RGBA',(16,16));pix=im.load();r=random.Random(name)
 for y in range(16):
  for x in range(16):
   offset=r.randint(-7,7)+(6 if x%4==0 else -4 if x%4==3 else 0)
   if kind=='metal':offset=r.randint(-8,8)+(14 if y in (1,2) else -12 if y in (14,15) else 0)
   pix[x,y]=tuple(max(0,min(255,c+offset)) for c in base)+(255,)
 save(name,im)
 return im
for name,base in [('grape_mash_red',(105,56,119)),('grape_mash_white',(145,160,74)),('grape_mash_rose',(154,84,105)),('wine_band',(66,55,46)),('grape_press_metal',(104,109,112)),('wine_cork',(151,111,63))]:tile(name,base,'metal' if 'metal' in name or 'band' in name else 'wood')
for kind,base in [('red',(119,42,86)),('white',(180,163,76)),('rose',(172,78,103))]:
 im=Image.new('RGBA',(16,16),base+(255,));d=ImageDraw.Draw(im)
 for y in (3,9,14):d.line((2,y,6,y),fill=tuple(min(255,c+20) for c in base)+(255,))
 save('grape_juice_'+kind,im)
for name,base in [('wine_red',(65,36,57)),('wine_white',(95,116,51)),('wine_rose',(127,71,64))]:
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 for x in range(16):
  lum=1.25 if x in (2,3) else 1.06 if x<8 else .74
  d.line((x,0,x,15),fill=tuple(min(255,int(c*lum)) for c in base)+(255,))
 d.line((2,1,2,14),fill=(174,182,145,255));save(name,im)
for name,color in [('grape_red',(111,59,135)),('grape_white',(148,168,73)),('grape_green',(92,128,58)),('grape_late_red',(76,41,106)),('grape_late_white',(172,155,69)),('grape_overripe',(106,75,63))]:
 im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
 for x,y in [(6,1),(2,5),(9,5),(5,9),(6,12)]:
  d.rectangle((x,y,x+4,y+4),fill=tuple(max(0,c-20) for c in color)+(255,));d.rectangle((x,y,x+3,y+3),fill=color+(255,));d.point((x+1,y+1),fill=tuple(min(255,c+47) for c in color)+(255,))
 d.line((7,0,7,2),fill=(82,114,39,255));save(name,im)
 if name in ('grape_red','grape_white'):save('red_grapes' if name=='grape_red' else 'white_grapes',im,'item')
leaf=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(leaf)
d.polygon([(8,0),(10,4),(15,3),(13,8),(15,10),(10,13),(8,16),(5,12),(0,11),(3,7),(1,3),(6,4)],fill=(67,113,49,255));d.line((8,1,8,15),fill=(104,140,57,255));d.line((2,5,8,9,14,5),fill=(85,133,60,255));save('grape_leaf',leaf)
for red in (True,False):
 im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im);d.line((5,14,8,3),fill=(115,79,46,255),width=2);d.polygon([(7,6),(11,2),(14,3),(11,7)],fill=(83,133,52,255));d.polygon([(7,9),(3,6),(1,8),(5,11)],fill=(68,110,40,255));d.rectangle((9,8,12,10),fill=(125,66,143,255) if red else (162,176,76,255));save('red_grape_cutting' if red else 'white_grape_cutting',im,'item')
yeast=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(yeast);d.rectangle((4,2,12,14),fill=(117,80,45,255));d.rectangle((5,3,11,13),fill=(222,195,135,255));d.rectangle((5,3,11,4),fill=(153,111,65,255));d.rectangle((5,8,11,12),fill=(233,222,174,255));d.point((7,9),fill=(113,86,45,255));d.point((9,10),fill=(113,86,45,255));save('wine_yeast',yeast,'item')
must=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(must);d.polygon([(2,3),(13,3),(12,13),(4,13)],fill=(74,79,82,255));d.polygon([(3,4),(12,4),(11,12),(5,12)],fill=(155,159,159,255));d.ellipse((2,1,13,6),fill=(194,199,195,255));d.ellipse((3,2,12,5),fill=(112,49,116,255));d.line((4,7,5,11),fill=(226,230,219,255));save('grape_must',must,'item')
label=Image.new('RGBA',(16,16),(236,221,178,255));d=ImageDraw.Draw(label);d.rectangle((1,1,14,14),outline=(130,97,56,255));d.rectangle((3,11,12,12),fill=(170,142,92,255));d.ellipse((5,4,8,7),fill=(98,57,115,255));d.ellipse((8,4,11,7),fill=(117,65,128,255));d.ellipse((7,7,10,10),fill=(89,45,112,255));d.line((8,2,8,4),fill=(88,120,53,255));save('wine_label',label)
# Models use vanilla timber with our original grape, bottle, label and metal sprites.
WOOD='minecraft:block/oak_planks'; DARK='minecraft:block/spruce_planks'
def box(a,b,texture='wood',uv=None,faces=None,rotation=None):
 obj={'from':a,'to':b,'faces':{f:{'texture':'#'+texture,**({'uv':uv} if uv else {})} for f in (faces or ['north','south','east','west','up','down'])}}
 if rotation:obj['rotation']=rotation
 return obj
def model(name,elements,textures,display=None):
 o={'parent':'minecraft:block/block','textures':{'particle':textures.get('wood',next(iter(textures.values()))),**textures},'elements':elements}
 if display:o['display']=display
 write(ASSET/f'models/block/{name}.json',o)
def block_item(name,parent=None):write(ASSET/f'models/item/{name}.json',{'parent':'hobbymod:block/'+(parent or name)})
def item(name):write(ASSET/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'hobbymod:item/'+name}})
def facing_variants(name):write(ASSET/f'blockstates/{name}.json',{'variants':{f'facing={f}':{'model':'hobbymod:block/'+name,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})
frame=[box([1,0,7],[3,16,9]),box([13,0,7],[15,16,9]),box([1,4,7],[15,5,9]),box([1,12,7],[15,13,9])]
model('grape_trellis',frame,{'wood':WOOD});block_item('grape_trellis')
variants={}
for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:variants[f'vine=empty,facing={facing}']={'model':'hobbymod:block/grape_trellis','y':rot}
for color in ('red','white'):
 for age in range(7):
  stem_height=3 if age==0 else 8 if age==1 else 16
  es=frame+[box([7.25,0,6.5],[8.25,stem_height,7.5],'stem')]
  for x,y in [(4,2),(8,6),(3,10),(10,10)][:1 if age==0 else 2 if age==1 else 4]:
   es.append(box([x,y,6.4],[x+4,min(16,y+4),6.4],'leaf',[0,0,16,16],['north','south']))
  grape_tex='grape_green' if age<4 else 'grape_'+color if age==4 else 'grape_late_'+color if age==5 else 'grape_overripe'
  if age>=3:
   for x,y in [(4,5),(10,8),(5,11)]:
    es.extend([box([x,y,5.1],[x+2,y+2,7.1],'fruit',[4,5,8,9]),box([x+1.4,y-1.4,5.5],[x+3.4,y+.6,7.5],'fruit',[4,5,8,9]),box([x-.2,y-2.8,5.3],[x+1.8,y-.8,7.3],'fruit',[4,5,8,9])])
  name=f'grape_vine_{color}_{age}';model(name,es,{'wood':WOOD,'stem':'minecraft:block/oak_log','leaf':'hobbymod:block/grape_leaf','fruit':'hobbymod:block/'+grape_tex})
  for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:variants[f'vine={color},age={age},facing={facing}']={'model':'hobbymod:block/'+name,'y':rot}
write(ASSET/'blockstates/grape_trellis.json',{'variants':variants})
press=[box([1,0,1],[15,2,15]),box([1,0,6],[3,16,10]),box([13,0,6],[15,16,10]),box([1,14,6],[15,16,10]),box([7.4,10,7.4],[8.6,14,8.6],'metal',[0,0,16,16]),box([4,15.5,7.25],[12,16.5,8.75],'metal'),box([3,2,3],[13,3,13]),box([2,3,3],[3.5,10,13]),box([12.5,3,3],[14,10,13]),box([3,3,2],[13,10,3.5]),box([3,3,12.5],[13,10,14])]
for y in (3,8):
 for a,b in [([1.9,y,2.9],[3.6,y+1,13.1]),([12.4,y,2.9],[14.1,y+1,13.1]),([2.9,y,1.9],[13.1,y+1,3.6]),([2.9,y,12.4],[13.1,y+1,14.1])]:press.append(box(a,b,'band'))
model('grape_press',press,{'wood':WOOD,'metal':'hobbymod:block/grape_press_metal','band':'hobbymod:block/wine_band'});facing_variants('grape_press')
model('grape_press_inventory',press+[box([3,11,3],[13,12,13])],{'wood':WOOD,'metal':'hobbymod:block/grape_press_metal','band':'hobbymod:block/wine_band'});block_item('grape_press','grape_press_inventory')
barrel=[box([2,0,2],[14,16,14]),box([1,2,2],[15,14,14]),box([2,2,1],[14,14,15]),box([6.5,4,0],[9.5,6,3],'tap'),box([7,6,-.5],[9,8,1.5],'tap'),box([7,15,7],[9,18,9],'glass',[0,0,16,16])]
for y in (2,12):
 barrel.extend([box([.9,y,1.9],[15.1,y+1,14.1],'band'),box([1.9,y,.9],[14.1,y+1,15.1],'band')])
model('fermentation_barrel',barrel,{'wood':DARK,'band':'hobbymod:block/wine_band','tap':'minecraft:block/copper_block','glass':'minecraft:block/glass'});facing_variants('fermentation_barrel');block_item('fermentation_barrel')
tub=[box([0,0,0],[16,2,16]),box([0,2,0],[1,6,16]),box([15,2,0],[16,6,16]),box([1,2,0],[15,6,1]),box([1,2,15],[15,6,16])]
model('grape_treading_tub',tub,{'wood':DARK,'progress':'minecraft:block/white_concrete',**{'juice_'+kind:'hobbymod:block/grape_juice_'+kind for kind in ('red','white','rose')},**{'mash_'+kind:'hobbymod:block/grape_mash_'+kind for kind in ('red','white','rose')}});facing_variants('grape_treading_tub');block_item('grape_treading_tub')
rack=[]
for y in (0,8,15):rack.append(box([1,y,2],[15,y+1,14]))
for x in (1,7.5,14):rack.append(box([x,0,2],[x+1,16,14]))
model('wine_rack',rack,{'wood':DARK});facing_variants('wine_rack');block_item('wine_rack');block_item('portable_wine_cask','fermentation_barrel')
for color in ('red','white','rose'):
 bottle=[box([5,1,5],[11,10,11],'wine',[0,0,16,16]),box([6,10,6],[10,11,10],'wine'),box([7,11,7],[9,15,9],'wine',[0,0,16,16]),box([7,15,7],[9,16,9],'cork'),box([5.25,4,4.98],[10.75,8,4.98],'label',[0,0,16,16],['north']),box([5.25,4,11.02],[10.75,8,11.02],'label',[0,0,16,16],['south'])]
 model('wine_bottle_'+color,bottle,{'wine':'hobbymod:block/wine_'+color,'cork':'hobbymod:block/wine_cork','label':'hobbymod:block/wine_label'}, {'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.9,.9,.9]},'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.5,.5,.5]}})
 write(ASSET/f'models/item/wine_bottle_{color}.json',{'parent':'hobbymod:block/wine_bottle_'+color})
write(ASSET/'models/item/wine_bottle.json',{'parent':'hobbymod:block/wine_bottle_red','overrides':[{'predicate':{'hobbymod:wine_color':.5},'model':'hobbymod:item/wine_bottle_white'},{'predicate':{'hobbymod:wine_color':1},'model':'hobbymod:item/wine_bottle_rose'}]})
for name in ('red_grapes','white_grapes','red_grape_cutting','white_grape_cutting','wine_yeast','grape_must'):item(name)
def ingredient(id):return {'tag':id[1:]} if id.startswith('#') else {'item':id}
def recipe(name,pattern,keys,result,count=1):write(DATA/f'hobbymod/recipe/{name}.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':{k:ingredient(v) for k,v in keys.items()},'result':{'id':result,'count':count}})
def shapeless(name,ingredients,result,count=1):write(DATA/f'hobbymod/recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[ingredient(i) for i in ingredients],'result':{'id':result,'count':count}})
recipe('grape_trellis',['S S','SPS','S S'],{'S':'minecraft:stick','P':'#minecraft:planks'},'hobbymod:grape_trellis',2)
recipe('grape_treading_tub',['P P','PPP'],{'P':'#minecraft:planks'},'hobbymod:grape_treading_tub')
recipe('grape_press',[' I ','PPP','SBS'],{'I':'minecraft:iron_ingot','P':'#minecraft:planks','S':'#minecraft:wooden_slabs','B':'minecraft:piston'},'hobbymod:grape_press')
shapeless('fermentation_barrel',['minecraft:barrel','minecraft:copper_ingot','minecraft:glass_bottle'],'hobbymod:fermentation_barrel')
recipe('wine_rack',['PPP','SSS','PPP'],{'P':'#minecraft:planks','S':'minecraft:stick'},'hobbymod:wine_rack')
shapeless('wine_yeast',['minecraft:bread','minecraft:sugar'],'hobbymod:wine_yeast',4)
for color,fruit in [('red','minecraft:sweet_berries'),('white','minecraft:glow_berries')]:
 shapeless(color+'_grape_cutting',[fruit,'minecraft:stick'],'hobbymod:'+color+'_grape_cutting',2)
 shapeless(color+'_grape_cutting_from_grapes',['hobbymod:'+color+'_grapes','minecraft:stick'],'hobbymod:'+color+'_grape_cutting',2)
for name in ('grape_press','grape_treading_tub','fermentation_barrel','wine_rack'):
 write(DATA/f'hobbymod/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'hobbymod:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for color in ('red','white'):
 write(DATA/f'c/tags/item/fruits/{color}_grapes.json',{'replace':False,'values':['hobbymod:'+color+'_grapes',{'id':'vinery:'+color+'_grape','required':False}]})
write(DATA/'c/tags/item/fruits/grapes.json',{'replace':False,'values':['#c:fruits/red_grapes','#c:fruits/white_grapes']})
write(DATA/'c/tags/item/drinks/wine.json',{'replace':False,'values':['hobbymod:wine_bottle']})
p=DATA/'minecraft/tags/block/mineable/axe.json';tag=json.loads(p.read_text()) if p.exists() else {'replace':False,'values':[]}
for name in ('grape_trellis','grape_press','grape_treading_tub','fermentation_barrel','wine_rack'):
 if 'hobbymod:'+name not in tag['values']:tag['values'].append('hobbymod:'+name)
write(p,tag)
p=ASSET/'lang/en_us.json';lang=json.loads(p.read_text()) if p.exists() else {}
for id,name in [('grape_treading_tub','Grape Treading Tub'),('grape_trellis','Vineyard Trellis'),('grape_press','Grape Press'),('fermentation_barrel','Fermentation Barrel'),('wine_rack','Wine Rack')]:lang['block.hobbymod.'+id]=name
for id,name in [('red_grapes','Red Grapes'),('white_grapes','White Grapes'),('red_grape_cutting','Red Grape Cutting'),('white_grape_cutting','White Grape Cutting'),('wine_yeast','Wine Yeast'),('grape_must','Grape Must'),('portable_wine_cask','Portable Wine Cask'),('wine_bottle','Wine Bottle')]:lang['item.hobbymod.'+id]=name
write(p,lang)
print('Generated winery models, original pixel textures, recipes and tags.')
# Unlock the small crafting family from its first supplies in the vanilla recipe book.
for recipe_id,material in {
 'grape_treading_tub':'minecraft:stick','grape_trellis':'minecraft:stick','red_grape_cutting':'minecraft:sweet_berries',
 'white_grape_cutting':'minecraft:glow_berries','red_grape_cutting_from_grapes':'hobbymod:red_grapes',
 'white_grape_cutting_from_grapes':'hobbymod:white_grapes','grape_press':'hobbymod:red_grapes',
 'fermentation_barrel':'minecraft:barrel','wine_rack':'hobbymod:wine_bottle','wine_yeast':'minecraft:bread'
}.items():
 write(DATA/f'hobbymod/advancement/recipes/{recipe_id}.json',{'parent':'minecraft:recipes/root','criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[material]}]}}},'requirements':[['material']],'rewards':{'recipes':['hobbymod:'+recipe_id]}})
