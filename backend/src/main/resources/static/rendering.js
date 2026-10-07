export const BRAND_TOP='#사가일터선교';
export const BRAND_BOTTOM='Business Is Mission';
export function dimensions(ratio){return ratio==='9:16'?[1080,1920]:[1280,720];}
export const SHORTS_SAFE_AREA={left:.07,top:.08,right:.76,bottom:.76};
const analyses=new WeakMap();
function skin(r,g,b){return r>65&&r>g*1.08&&r>b*1.2&&Math.max(r,g,b)-Math.min(r,g,b)>20&&r-g<110;}
function inspect(image){
 if(analyses.has(image))return analyses.get(image);
 const iw=image.naturalWidth||image.width,ih=image.naturalHeight||image.height,c=document.createElement('canvas');c.width=160;c.height=Math.max(40,Math.round(160*ih/iw));const ctx=c.getContext('2d');ctx.drawImage(image,0,0,c.width,c.height);const {data}=ctx.getImageData(0,0,c.width,c.height),w=c.width,h=c.height;
 const pixel=(x,y)=>Array.from(data.slice((y*w+x)*4,(y*w+x)*4+3));const distance=(a,b)=>a.reduce((n,v,i)=>n+Math.abs(v-b[i]),0);
 const uniform=(vertical,index,ref)=>{let same=0,total=vertical?h:w;for(let j=0;j<total;j++)if(distance(pixel(vertical?index:j,vertical?j:index),ref)<22)same++;return same/total>.97;};
 let left=0,right=w,top=0,bottom=h;
 // Trim paired uniform padding bands; a uniformly colored image stays intact.
 for(const vertical of [false,true]){const n=vertical?w:h,ref=pixel(0,0);if(distance(ref,pixel(vertical?w-1:0,vertical?0:h-1))>22)continue;let a=0,b=n;while(a<n*.4&&uniform(vertical,a,ref))a++;while(b>n*.6&&uniform(vertical,b-1,ref))b--;if(a>n*.02&&a<n*.4&&n-b>n*.02&&b>n*.6){if(vertical){left=a;right=b;}else{top=a;bottom=b;}}}
 const bounds={x:left/w*iw,y:top/h*ih,w:(right-left)/w*iw,h:(bottom-top)/h*ih};let weight=0,cx=0;const cells=[];
 for(let y=top+1;y<bottom-1;y+=2)for(let x=left+1;x<right-1;x+=2){const p=pixel(x,y),edge=(distance(p,pixel(x+1,y))+distance(p,pixel(x,y+1)))/255,face=skin(...p)?2:0,v=edge+face,nx=(x-left)/(right-left),ny=(y-top)/(bottom-top);cells.push({x:nx,y:ny,v,skin:face>0});weight+=v;cx+=nx*v;}
 // Local fallback: compact skin-colored regions in the upper image are protected
 // as possible faces. This is a visual heuristic, not semantic/AI face recognition.
 const mask=new Set();for(let y=top+1;y<bottom*.72;y+=2)for(let x=left+1;x<right-1;x+=2)if(skin(...pixel(x,y)))mask.add(y*w+x);
 const guesses=[];while(mask.size){const seed=mask.values().next().value,stack=[seed];mask.delete(seed);let x1=w,x2=0,y1=h,y2=0,n=0;while(stack.length){const p=stack.pop(),x=p%w,y=Math.floor(p/w);n++;x1=Math.min(x1,x);x2=Math.max(x2,x);y1=Math.min(y1,y);y2=Math.max(y2,y);for(const dx of [-2,0,2])for(const dy of [-2,0,2]){const next=(y+dy)*w+x+dx;if(mask.delete(next))stack.push(next);}}
  const bw=x2-x1+2,bh=y2-y1+2,area=bw*bh,aspect=bw/bh;if(n>=8&&area>w*h*.001&&area<w*h*.16&&aspect>.4&&aspect<2.2&&(y1+y2)/2<h*.58)guesses.push({x:x1/w*iw,y:y1/h*ih,width:bw/w*iw,height:bh/h*ih,score:n/Math.sqrt(area)});
 }
 const faces=guesses.sort((a,b)=>b.score-a.score).slice(0,4),result={bounds,cells,focalX:faces.length?Math.min(.9,Math.max(.1,(faces[0].x+faces[0].width/2-bounds.x)/bounds.w)):weight?Math.min(.85,Math.max(.15,cx/weight)):.5,faces,method:'visual'};analyses.set(image,result);return result;
}
export async function prepareImage(image){
 const a=inspect(image);if(typeof globalThis.FaceDetector==='function'&&!a.faceAttempted){a.faceAttempted=true;try{const faces=await new globalThis.FaceDetector({fastMode:true,maxDetectedFaces:6}).detect(image);if(faces.length){a.faces=faces.map(f=>f.boundingBox);a.method='face';const face=faces.sort((x,y)=>y.boundingBox.width*y.boundingBox.height-x.boundingBox.width*x.boundingBox.height)[0].boundingBox;a.focalX=Math.min(.9,Math.max(.1,(face.x+face.width/2-a.bounds.x)/a.bounds.w));}}catch{}}
 return image;
}
function lines(ctx,text,max){const result=[],chars=Array.from(text);let start=0;while(start<chars.length){while(start<chars.length&&/\s/.test(chars[start]))start++;if(start===chars.length)break;let end=start+1;while(end<chars.length&&ctx.measureText(chars.slice(start,end+1).join('')).width<=max)end++;if(end<chars.length){let space=end-1;while(space>start&&!/\s/.test(chars[space]))space--;if(space>start)end=space;}result.push({text:chars.slice(start,end).join(''),start});start=end;}return result;}
export function portraitCopy(concept){const chars=Array.from(concept.portraitHeadline||concept.headline);return chars.length>24?chars.slice(0,23).join('').trimEnd()+'…':chars.join('');}
export function compose(canvas,{image,concept,template,ratio,focalX}){
 const [w,h]=dimensions(ratio);canvas.width=w;canvas.height=h;const ctx=canvas.getContext('2d'),portrait=ratio==='9:16',a=inspect(image),b=a.bounds;
 const scale=Math.max(w/b.w,h/b.h),cropW=w/scale,cropH=h/scale,fx=focalX??(portrait?a.focalX:.5),sx=b.x+Math.max(0,Math.min(b.w-cropW,b.w*fx-cropW/2)),sy=b.y+(b.h-cropH)/2;
 ctx.drawImage(image,sx,sy,cropW,cropH,0,0,w,h);
 // Dedicated Shorts template: protected left text zone, never a rotated wide layout.
 const safe=SHORTS_SAFE_AREA,margin=w*(portrait?safe.left+.02:.055),text=portrait?portraitCopy(concept):concept.headline,maxWidth=w*(portrait?safe.right-safe.left-.04:template.layout==='left'?.52:.88);let size=w*(portrait?.14:template.layout==='left'?.071:.081)*(concept.titleScale||1),wrapped;const font=concept.titleFont||template.font||'sans-serif',weight=concept.titleWeight||'900';
 do{ctx.font=`${weight} ${size}px ${font}`;wrapped=lines(ctx,text,maxWidth);if(wrapped.length<= (portrait?2:4)&&wrapped.length*size*(portrait?1.12:1.2)<=h*(portrait?.28:.45))break;size-=2;}while(size>w*(portrait?.035:.027));
 const lineHeight=size*(portrait?1.12:1.2),blockHeight=wrapped.length*lineHeight;
 const candidates={top:h*.18,center:h*.44-blockHeight/2};let position=portrait?(concept.portraitPosition||'auto'):template.layout;
 if(portrait&&!['auto','top','center'].includes(position))position='center';
 if(portrait&&position==='auto'){
  const scores=Object.entries(candidates).map(([name,y])=>{let score=name==='top'?-1:0;for(const cell of a.cells){const px=(b.x+cell.x*b.w-sx)/cropW*w,py=(b.y+cell.y*b.h-sy)/cropH*h;if(px>=margin&&px<=margin+maxWidth&&py>y-size*.18&&py<y+blockHeight+size*.18)score+=cell.v*(cell.skin?4:1);}
   for(const face of a.faces){const x=(face.x-sx)/cropW*w,fy=(face.y-sy)/cropH*h,fw=face.width/cropW*w,fh=face.height/cropH*h;if(x+fw>margin&&x<margin+maxWidth&&fy+fh>y&&fy<y+blockHeight)score+=100000;}return {name,score};});position=scores.sort((x,y)=>x.score-y.score)[0].name;
 }
 let top=portrait?(candidates[position]??candidates.top):template.layout==='bottom'?h*.82-blockHeight:template.layout==='center'?h*.64-blockHeight/2:h*.5-blockHeight/2;
 if(!portrait&&concept.titlePosition&&concept.titlePosition!=='auto')top=concept.titlePosition==='top'?h*.18:concept.titlePosition==='bottom'?h*.82-blockHeight:h*.5-blockHeight/2;
 // Gradients blend into the photograph; no opaque title or brand bars.
 const edge=ctx.createLinearGradient(0,0,0,h);edge.addColorStop(0,'rgba(0,0,0,.45)');edge.addColorStop(.16,'rgba(0,0,0,0)');edge.addColorStop(.82,'rgba(0,0,0,0)');edge.addColorStop(1,'rgba(0,0,0,.4)');ctx.fillStyle=edge;ctx.fillRect(0,0,w,h);
 const gradient=portrait?ctx.createLinearGradient(0,Math.max(0,top-h*.1),0,Math.min(h,top+blockHeight+h*.1)):template.layout==='left'?ctx.createLinearGradient(0,0,w*.85,0):ctx.createLinearGradient(0,Math.max(0,top-h*.2),0,h);
 gradient.addColorStop(0,portrait?'rgba(0,0,0,0)':'rgba(0,0,0,.48)');gradient.addColorStop(portrait?.45:1,portrait?'rgba(0,0,0,.48)':'rgba(0,0,0,0)');if(portrait)gradient.addColorStop(1,'rgba(0,0,0,0)');ctx.fillStyle=gradient;ctx.fillRect(0,0,w,h);
 ctx.shadowColor='rgba(0,0,0,.7)';ctx.shadowBlur=w*.009;ctx.shadowOffsetY=w*.003;ctx.textBaseline='middle';ctx.fillStyle=concept.brandColor||template.brandText||template.text;const drawBrand=(text,y,fontSize,max)=>{ctx.font=`700 ${fontSize}px sans-serif`;while(ctx.measureText(text).width>max&&fontSize>8){fontSize-=1;ctx.font=`700 ${fontSize}px sans-serif`;}ctx.fillText(text,margin,y,max);};drawBrand(template.brandTop||BRAND_TOP,h*(portrait?.12:.065),w*(portrait?.037:.024),w*(portrait?.65:.86));drawBrand(template.brandBottom||BRAND_BOTTOM,h*(portrait?.70:.945),w*(portrait?.032:.022),w*(portrait?.65:.82));
 ctx.font=`${weight} ${size}px ${font}`;ctx.textBaseline='top';ctx.lineJoin='round';ctx.strokeStyle='rgba(0,0,0,.3)';ctx.lineWidth=size*.022;
 const emphasis=concept.emphasis||'',start=text.indexOf(emphasis),prefix=Array.from(text.slice(0,Math.max(0,start))).length,end=prefix+Array.from(emphasis).length;
 wrapped.forEach((line,i)=>{const lineWidth=ctx.measureText(line.text).width,align=concept.titleAlign||(!portrait&&template.layout==='center'?'center':'left');let x=align==='right'?margin+maxWidth-lineWidth:align==='center'?(portrait?margin+(maxWidth-lineWidth)/2:(w-lineWidth)/2):margin;const y=top+i*lineHeight;ctx.strokeText(line.text,x,y);Array.from(line.text).forEach((ch,j)=>{ctx.fillStyle=concept.titleColor|| (emphasis&&start>=0&&line.start+j>=prefix&&line.start+j<end?template.accent:template.text);ctx.fillText(ch,x,y);x+=ctx.measureText(ch).width;});});
 canvas.dataset.textPosition=position;canvas.dataset.titleSize=String(size);canvas.dataset.titleLines=String(wrapped.length);canvas.dataset.positionMethod=a.method;
 return canvas;
}
