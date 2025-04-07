import React, { useState, useEffect } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const CommentComponent = ({ scriptId, currentUser }) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [stompClient, setStompClient] = useState(null);

    useEffect(() => {
        // Connexion WebSocket
        const socket = new SockJS('/ws');
        const client = Stomp.over(socket);
        
        client.connect({}, () => {
            // S'abonner aux commentaires du script
            client.subscribe(`/topic/script.${scriptId}.comments`, (message) => {
                const comment = JSON.parse(message.body);
                setComments(prevComments => [...prevComments, comment]);
            });
        });

        setStompClient(client);

        return () => {
            if (client) {
                client.disconnect();
            }
        };
    }, [scriptId]);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!newComment.trim()) return;

        const commentDto = {
            content: newComment,
            script: { id: scriptId },
            user: currentUser
        };

        // Envoyer le commentaire
        stompClient.send("/app/comment.add", {}, JSON.stringify(commentDto));
        setNewComment('');
    };

    return (
        <div className="comment-section">
            <h3>Commentaires</h3>
            
            {/* Liste des commentaires */}
            <div className="comments-list">
                {comments.map((comment, index) => (
                    <div key={index} className="comment">
                        <p><strong>{comment.user.firstname} {comment.user.lastname}</strong></p>
                        <p>{comment.content}</p>
                        <small>{new Date(comment.createdAt).toLocaleString()}</small>
                    </div>
                ))}
            </div>

            {/* Formulaire d'ajout de commentaire */}
            <form onSubmit={handleSubmit}>
                <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder="Ajouter un commentaire..."
                />
                <button type="submit">Envoyer</button>
            </form>
        </div>
    );
};

export default CommentComponent; 