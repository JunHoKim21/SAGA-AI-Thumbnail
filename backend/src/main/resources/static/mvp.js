document.addEventListener('DOMContentLoaded', () => {
    const videoFileInput = document.getElementById('video-file');
    const dropzone = document.getElementById('dropzone');
    const selectedFileInfo = document.getElementById('selected-file-info');
    
    const btnExtractImage = document.getElementById('btn-extract-image');
    const btnExtractVideo = document.getElementById('btn-extract-video');
    const notice = document.getElementById('notice');
    
    const loadingIndicator = document.getElementById('loading-indicator');
    const resultContainer = document.getElementById('result-container');

    let currentFile = null;

    // 드래그 앤 드롭 이벤트 처리
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, () => dropzone.classList.add('dragover'), false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, () => dropzone.classList.remove('dragover'), false);
    });

    dropzone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        handleFiles(files);
    });

    videoFileInput.addEventListener('change', function() {
        handleFiles(this.files);
    });

    function handleFiles(files) {
        if (files.length === 0) return;
        
        const file = files[0];
        if (!file.name.toLowerCase().endsWith('.mp4')) {
            showNotice('MP4 동영상 파일만 선택해 주세요.', true);
            return;
        }

        currentFile = file;
        selectedFileInfo.textContent = `선택된 파일: ${file.name} (${(file.size / (1024 * 1024)).toFixed(2)} MB)`;
        
        btnExtractImage.disabled = false;
        btnExtractVideo.disabled = false;
        showNotice('');
    }

    function showNotice(msg, isError = false) {
        notice.textContent = msg;
        if (isError) {
            notice.classList.add('error');
        } else {
            notice.classList.remove('error');
        }
    }

    function getSelectedRatio() {
        return document.querySelector('input[name="ratio"]:checked').value;
    }

    btnExtractImage.addEventListener('click', async () => {
        if (!currentFile) return;
        await processMedia('/api/v1/thumbnail/image', 'image');
    });

    btnExtractVideo.addEventListener('click', async () => {
        if (!currentFile) return;
        await processMedia('/api/v1/thumbnail/video', 'video');
    });

    async function processMedia(url, type) {
        const ratio = getSelectedRatio();
        const formData = new FormData();
        formData.append('video', currentFile);
        formData.append('ratio', ratio);

        btnExtractImage.disabled = true;
        btnExtractVideo.disabled = true;
        loadingIndicator.style.display = 'block';
        resultContainer.style.display = 'none';
        showNotice(`AI 분석 및 ${type === 'image' ? '썸네일' : '하이라이트'} 생성 중...`);

        try {
            const response = await fetch(url, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                let errorMsg = `서버 오류가 발생했습니다. (HTTP ${response.status})`;
                try {
                    const errorJson = await response.json();
                    if (errorJson && errorJson.error) {
                        errorMsg = errorJson.error;
                    }
                } catch (e) {
                    // JSON이 아닌 경우 기본 메시지 유지
                }
                throw new Error(errorMsg);
            }

            const blob = await response.blob();
            const objectUrl = URL.createObjectURL(blob);
            
            displayResult(objectUrl, type, ratio);
            showNotice('생성이 완료되었습니다!');

        } catch (error) {
            console.error('Error:', error);
            showNotice(`처리 중 오류가 발생했습니다: ${error.message}`, true);
        } finally {
            btnExtractImage.disabled = false;
            btnExtractVideo.disabled = false;
            loadingIndicator.style.display = 'none';
        }
    }

    function displayResult(url, type, ratio) {
        resultContainer.innerHTML = ''; // 초기화
        resultContainer.style.display = 'flex';

        const title = document.createElement('h3');
        title.textContent = type === 'image' ? `생성된 썸네일 (${ratio})` : `생성된 하이라이트 영상 (${ratio})`;
        resultContainer.appendChild(title);

        if (type === 'image') {
            const img = document.createElement('img');
            img.src = url;
            img.id = 'result-image';
            img.className = 'result-item';
            resultContainer.appendChild(img);

            const downloadBtn = document.createElement('a');
            downloadBtn.href = url;
            downloadBtn.download = 'thumbnail.jpg';
            downloadBtn.className = 'button';
            downloadBtn.style.textDecoration = 'none';
            downloadBtn.style.display = 'inline-block';
            downloadBtn.style.textAlign = 'center';
            downloadBtn.textContent = '이미지 다운로드';
            resultContainer.appendChild(downloadBtn);
        } else {
            const video = document.createElement('video');
            video.src = url;
            video.id = 'result-video';
            video.className = 'result-item';
            video.controls = true;
            resultContainer.appendChild(video);

            const downloadBtn = document.createElement('a');
            downloadBtn.href = url;
            downloadBtn.download = 'highlight.mp4';
            downloadBtn.className = 'button';
            downloadBtn.style.textDecoration = 'none';
            downloadBtn.style.display = 'inline-block';
            downloadBtn.style.textAlign = 'center';
            downloadBtn.textContent = '영상 다운로드';
            resultContainer.appendChild(downloadBtn);
        }
        
        // 결과 화면으로 스크롤
        resultContainer.scrollIntoView({ behavior: 'smooth' });
    }
});
