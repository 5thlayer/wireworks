# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, until an artist draws the real ones, in the style
# of Groundworks' publish/make-cover.py. Run from the repo root with Pillow installed,
# with the decompiled Minecraft sources (for the ground textures) at ../mc-26.1.2.109-src.
from PIL import Image, ImageDraw, ImageFont
T='../mc-26.1.2.109-src/assets/minecraft/textures/block/'
P='src/main/resources/assets/wireworks/textures/block/'
BG=(24,26,32)
ACCENT=(232,140,60)   # copper, the default wire
AREA=(255,214,90)     # a pole's supply area
FADE=40  # 0 leaves the scene at full strength, 255 hides it
POLES=['small_electric_pole','medium_electric_pole','substation_electric_pole','medium_electric_pole']
_cache={}
def tex(path,s):
    if (path,s) not in _cache:
        _cache[path,s]=Image.open(path).convert('RGBA').resize((s,s),Image.NEAREST)
    return _cache[path,s]
def wire(d,a,b,sag,w):
    # a wire hangs between two pole tops as a shallow parabola
    pts=[]
    for i in range(33):
        t=i/32
        pts.append((a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t+sag*4*t*(1-t)))
    d.line(pts,fill=ACCENT+(255,),width=w)
def make(W,H,out,title_size,s):
    img=Image.new('RGBA',(W,H),BG+(255,))
    ground=H-s
    for x in range(0,W,s):
        img.alpha_composite(tex(T+'dirt.png',s),(x,ground))
    # poles along the ground, each with its supply area drawn under it
    pitch=s*4; xs=list(range(s,W,pitch)); tops=[]
    over=Image.new('RGBA',(W,H),(0,0,0,0)); od=ImageDraw.Draw(over)
    for i,x in enumerate(xs):
        r=s*2
        od.rectangle([x+s//2-r,ground-s//3,x+s//2+r,ground+s//3],fill=AREA+(70,),outline=AREA+(200,),width=max(2,s//24))
    img=Image.alpha_composite(img,over)
    for i,x in enumerate(xs):
        name=POLES[i%len(POLES)]; height=2+(i%2)
        for k in range(height):
            img.alpha_composite(tex(P+name+'.png',s),(x,ground-(k+1)*s))
        tops.append((x+s//2,ground-height*s+s//4))
    img=Image.alpha_composite(img,Image.new('RGBA',(W,H),BG+(FADE,)))
    d=ImageDraw.Draw(img)
    for a,b in zip(tops,tops[1:]):
        wire(d,a,b,s*0.6,max(2,s//10))
    bh=int(title_size*1.6); cy=int(H*0.3)
    band=Image.new('RGBA',(W,H),(0,0,0,0)); bd=ImageDraw.Draw(band)
    bd.rectangle([0,cy-bh//2,W,cy+bh//2],fill=(16,17,22,225))
    img=Image.alpha_composite(img,band)
    d=ImageDraw.Draw(img)
    lt=max(3,title_size//25)
    d.rectangle([0,cy-bh//2,W,cy-bh//2+lt],fill=ACCENT); d.rectangle([0,cy+bh//2-lt,W,cy+bh//2],fill=ACCENT)
    f1=ImageFont.truetype('/System/Library/Fonts/Supplemental/Impact.ttf',title_size)
    f2=ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial Black.ttf',int(title_size*0.62))
    a,w='WIRE','works'
    b1=d.textbbox((0,0),a,font=f1); b2=d.textbbox((0,0),w,font=f2)
    w1=b1[2]-b1[0]; w2=b2[2]-b2[0]; h2=b2[3]-b2[1]
    px=int(title_size*0.16); gap=int(title_size*0.1)
    bw=w2+2*px; tot=w1+gap+bw; x=(W-tot)//2
    h1=b1[3]-b1[1]; y=cy-h1//2-b1[1]; sh=max(3,title_size//20)
    d.text((x+sh-b1[0],y+sh),a,font=f1,fill=(0,0,0))
    d.text((x-b1[0],y),a,font=f1,fill=ACCENT)
    bx=x+w1+gap; bt=cy-h1//2; bb=bt+h1
    d.rounded_rectangle([bx+sh,bt+sh,bx+bw+sh,bb+sh],radius=px,fill=(0,0,0))
    d.rounded_rectangle([bx,bt,bx+bw,bb],radius=px,fill=(240,240,236))
    ty=bt+(h1-h2)//2-b2[1]
    d.text((bx+px-b2[0],ty),w,font=f2,fill=BG)
    img.convert('RGB').save(out)
make(1280,640,'publish/wireworks-cover.png',150,80)
make(512,512,'publish/wireworks-icon.png',80,40)
