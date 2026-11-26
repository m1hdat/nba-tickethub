/* Mirrors Android SeatPerspective geometry; Canvas mesh simulates its projective Matrix. */
(() => {
    'use strict';
    const textures = new Map();
    function camera(data, info) {
        const q = data.seats[info.index][3];
        const eyeX = ((q[0]+q[2]+q[4]+q[6])/4-data.world[0]/2)/data.court[0]*94;
        const eyeY = ((q[1]+q[3]+q[5]+q[7])/4-data.world[1]/2)/data.court[1]*50;
        const eyeHeight = 4 + (info.levelOrder-1)*20 + info.rowOrder*.7;
        const distance = Math.hypot(eyeX, eyeY, eyeHeight);
        const fx = -eyeX/distance, fy = -eyeY/distance, fz = -eyeHeight/distance;
        const horizontal = Math.hypot(fx, fy), rx = -fy/horizontal, ry = fx/horizontal;
        return {eyeX, eyeY, eyeHeight, fx, fy, fz, rx, ry, ux: -fz*ry, uy: fz*rx, uz: fx*ry-fy*rx};
    }
    function project(c, x, y, z = 0) {
        const dx = x-47-c.eyeX, dy = y-25-c.eyeY, dz = z-c.eyeHeight;
        const depth = Math.max(.1, dx*c.fx+dy*c.fy+dz*c.fz);
        return [.5+.68*(dx*c.rx+dy*c.ry)/depth, .54-1.21*(dx*c.ux+dy*c.uy+dz*c.uz)/depth];
    }
    function texture(theme) {
        if (!textures.has(theme.id)) {
            const image = document.createElement('canvas'); image.width = 816; image.height = 464;
            const ctx = image.getContext('2d'); ctx.setTransform(8,0,0,8,32,32);
            window.MobileArenaRenderer.court(ctx, theme);
            textures.set(theme.id, image);
        }
        return textures.get(theme.id);
    }
    function triangle(ctx, image, from, to) {
        const [s0,s1,s2] = from, [d0,d1,d2] = to;
        const x1 = s1[0]-s0[0], x2 = s2[0]-s0[0], y1 = s1[1]-s0[1], y2 = s2[1]-s0[1];
        const det = x1*y2-x2*y1;
        const a = ((d1[0]-d0[0])*y2-(d2[0]-d0[0])*y1)/det;
        const c = ((d2[0]-d0[0])*x1-(d1[0]-d0[0])*x2)/det;
        const b = ((d1[1]-d0[1])*y2-(d2[1]-d0[1])*y1)/det;
        const d = ((d2[1]-d0[1])*x1-(d1[1]-d0[1])*x2)/det;
        ctx.save(); ctx.beginPath(); ctx.moveTo(...d0); ctx.lineTo(...d1); ctx.lineTo(...d2); ctx.closePath(); ctx.clip();
        ctx.transform(a,b,c,d,d0[0]-a*s0[0]-c*s0[1],d0[1]-b*s0[0]-d*s0[1]); ctx.drawImage(image,0,0); ctx.restore();
    }
    function draw(target, data, info, theme, width, height) {
        const dpr = Math.min(window.devicePixelRatio||1,2), ctx = target.getContext('2d');
        target.width = Math.round(width*dpr); target.height = Math.round(height*dpr);
        ctx.setTransform(dpr,0,0,dpr,0,0);
        ctx.fillStyle = '#10283f'; ctx.fillRect(0,0,width,height);
        ctx.fillStyle = '#233f56'; ctx.beginPath(); ctx.ellipse(width/2,height*.535,width*.7,height*.415,0,0,2*Math.PI); ctx.fill();
        ctx.strokeStyle = theme.primaryColor; ctx.lineWidth = Math.max(2,height*.035);
        ctx.beginPath(); ctx.ellipse(width/2,height*.65,width*.65,height*.45,0,0,2*Math.PI); ctx.stroke();
        if (info) {
            const c = camera(data, info);
            const point=(x,y,z)=>{const p=project(c,x,y,z);return [p[0]*width,p[1]*height];};
            ctx.strokeStyle='#64748b';ctx.lineWidth=Math.max(1,height*.006);
            for(let row=0;row<24;row++){
                ctx.beginPath();let started=false;
                for(let step=0;step<=120;step++){const a=step*Math.PI/60,x=47+(63+row*1.8)*Math.cos(a),y=25+(39+row*1.5)*Math.sin(a);
                    if((x-47)*c.eyeX+(y-25)*c.eyeY>0){started=false;continue;}
                    const p=point(x,y,row*1.1+2);if(started)ctx.lineTo(...p);else{ctx.moveTo(...p);started=true;}}
                ctx.stroke();
            }
            const image = texture(theme), nx = 12, ny = 8;
            const make = (x,y) => { const p = project(c,-4+102*x/nx,-4+58*y/ny); return [p[0]*width,p[1]*height]; };
            for (let y=0;y<ny;y++) for(let x=0;x<nx;x++) {
                const a=[x*image.width/nx,y*image.height/ny], b=[(x+1)*image.width/nx,y*image.height/ny];
                const e=[x*image.width/nx,(y+1)*image.height/ny], d=[(x+1)*image.width/nx,(y+1)*image.height/ny];
                const A=make(x,y),B=make(x+1,y),E=make(x,y+1),D=make(x+1,y+1);
                triangle(ctx,image,[a,b,d],[A,B,D]); triangle(ctx,image,[a,d,e],[A,D,E]);
            }
        }
        if(info){const c=camera(data,info),p=(x,y,z)=>{const v=project(c,x,y,z);return [v[0]*width,v[1]*height];};
            ctx.strokeStyle='#f0e8da';ctx.lineWidth=Math.max(1.5,height*.006);ctx.beginPath();
            const line=(a,b)=>{ctx.moveTo(...p(...a));ctx.lineTo(...p(...b));};
            for(const x of [4,90]){line([x,25,0],[x,25,10]);line([x,22,10],[x,28,10]);line([x,28,10],[x,28,12]);line([x,28,12],[x,22,12]);line([x,22,12],[x,22,10]);
                for(let i=0;i<20;i++){const a=i*Math.PI/10,b=(i+1)*Math.PI/10,r=x===4?5.3:88.7;line([r+Math.cos(a)*.75,25+Math.sin(a)*.75,10],[r+Math.cos(b)*.75,25+Math.sin(b)*.75,10]);}}
            ctx.stroke();}
        ctx.fillStyle = '#071b2b'; ctx.fillRect(0,height*.95,width,height*.05);
    }
    window.SeatPerspectivePreview = {camera,project,draw};
})();
