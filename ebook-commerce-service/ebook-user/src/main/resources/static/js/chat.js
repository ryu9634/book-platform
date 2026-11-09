// AI 도서 추천 답변을 글자 단위로 받아 그립니다.
// 이 파일이 없거나 ebook.chat.stream-enabled가 false면 챗봇 화면은 폼 전송으로 그대로 동작합니다.
(function () {
    const form = document.getElementById('chat-form');
    const log = document.getElementById('chat-log');
    if (!form || !log) {
        return;
    }
    const input = document.getElementById('question');
    const submit = form.querySelector('button[type=submit]');
    const tokenMeta = document.querySelector('meta[name=_csrf]');
    const headerMeta = document.querySelector('meta[name=_csrf_header]');

    function addBubble(className, who) {
        const line = document.createElement('div');
        line.className = 'chat-line ' + className;
        const whoLabel = document.createElement('span');
        whoLabel.className = 'chat-who';
        whoLabel.textContent = who;
        const text = document.createElement('p');
        text.className = 'chat-text';
        line.append(whoLabel, text);
        log.append(line);
        line.scrollIntoView({block: 'nearest'});
        return text;
    }

    // SSE 한 덩어리를 {event, text}로 바꿉니다. 조각은 JSON으로 와서 줄바꿈이 깨지지 않습니다.
    function parseEvent(raw) {
        let event = 'message';
        let data = '';
        raw.split('\n').forEach(function (line) {
            if (line.startsWith('event:')) {
                event = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
                data += line.slice(5).trim();
            }
        });
        let text = '';
        if (data) {
            try {
                text = JSON.parse(data).text || '';
            } catch (ignored) {
                text = '';
            }
        }
        return {event: event, text: text};
    }

    form.addEventListener('submit', async function (submitEvent) {
        const question = (input.value || '').trim();
        if (!question) {
            return;
        }
        submitEvent.preventDefault();
        submit.disabled = true;

        const empty = log.querySelector('.chat-empty');
        if (empty) {
            empty.remove();
        }
        addBubble('chat-mine', '나').textContent = question;
        const answer = addBubble('chat-bot', 'AI');
        answer.textContent = '답변을 쓰고 있습니다…';
        input.value = '';

        let finished = false;
        try {
            const headers = {'Content-Type': 'application/x-www-form-urlencoded'};
            if (tokenMeta && headerMeta) {
                headers[headerMeta.content] = tokenMeta.content;
            }
            const response = await fetch(form.dataset.streamUrl || '/chat/stream', {
                method: 'POST',
                headers: headers,
                body: new URLSearchParams({question: question})
            });
            if (!response.ok || !response.body) {
                throw new Error('stream not available');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            let firstChunk = true;
            while (true) {
                const read = await reader.read();
                if (read.done) {
                    break;
                }
                buffer += decoder.decode(read.value, {stream: true});
                let boundary = buffer.indexOf('\n\n');
                while (boundary >= 0) {
                    const parsed = parseEvent(buffer.slice(0, boundary));
                    buffer = buffer.slice(boundary + 2);
                    if (parsed.event === 'chunk') {
                        if (firstChunk) {
                            answer.textContent = '';
                            firstChunk = false;
                        }
                        answer.textContent += parsed.text;
                        answer.scrollIntoView({block: 'nearest'});
                    } else if (parsed.event === 'done') {
                        finished = true;
                    } else if (parsed.event === 'error') {
                        answer.textContent = parsed.text;
                        answer.classList.add('chat-error');
                    }
                    boundary = buffer.indexOf('\n\n');
                }
            }
        } catch (ignored) {
            answer.textContent = '답변을 받지 못했습니다. 잠시 후 다시 시도해 주세요.';
            answer.classList.add('chat-error');
        } finally {
            submit.disabled = false;
        }

        // 저장된 대화와 추천 도서 카드는 서버가 그립니다. 끝났을 때만 다시 읽습니다.
        if (finished) {
            window.location.reload();
        }
    });
})();
