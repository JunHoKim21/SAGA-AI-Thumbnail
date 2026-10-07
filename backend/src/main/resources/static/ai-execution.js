export function createFirstAIExecutor({confirm,post,onEvent=()=>{}}){
 const pending=new Set(),accepted=new Set();
 return async function execute(jobId,body,summary){
  if(pending.has(jobId)||accepted.has(jobId))return {status:'duplicate'};
  pending.add(jobId);
  try{
   if(await confirm(summary)!==true)return {status:'cancelled'};
   onEvent({event:'confirmation accepted'});onEvent({event:'job POST started'});
   const result=await post(jobId,body);
   if(result.httpStatus!==202||typeof result.job?.id!=='string'||result.job.mode!=='ai'||!['queued','processing','ready','error','cancelled'].includes(result.job.status)){
    const error=new Error('서버에서 AI 작업 등록을 확인하지 못했습니다. 추가 요청은 하지 않습니다.');error.httpStatus=result.httpStatus;throw error;
   }
   accepted.add(jobId);onEvent({event:'job POST accepted',httpStatus:202});return {status:'accepted',job:result.job};
  }catch(error){onEvent({event:'job POST rejected',httpStatus:Number.isInteger(error.httpStatus)?error.httpStatus:null});throw error;}
  finally{pending.delete(jobId);}
 };
}
