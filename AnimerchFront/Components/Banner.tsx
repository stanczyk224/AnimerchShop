import Berserk from '../public/Berserk_anime_logo.svg'
import './Banner.css'
function Banner() {
    return (
        <div className="banner">
            <img src={Berserk} alt="Berserk Anime Logo" />
            <p>Discover the epic world of Berserk!</p>
        </div>
    );
}

export default Banner;