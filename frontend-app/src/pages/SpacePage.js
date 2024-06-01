import { useParams } from "react-router-dom";
import spaces from "../spaces";
import { useState } from "react";
import {RangeCalendar, Button} from "@nextui-org/react";
import {today, getLocalTimeZone} from "@internationalized/date";
import { useNavigate } from "react-router-dom";

const SpacePage = () => {
    let now = today(getLocalTimeZone());
      
    let isDateUnavailable = (date) => date.compare(now) < 0
      
    let { id } = useParams();
    const item = spaces.find(item => item.id === parseInt(id));
    //const item = fetch(`http://localhost:8080/api/space/${id}`).then((response) => response.json());
    const [numberOfDays, setNumberOfDays] = useState(0);

    const calendarChangeHandler = (date) => {
    const startDate = new Date(date.start.year, date.start.month - 1, date.start.day);
    const endDate = new Date(date.end.year, date.end.month - 1, date.end.day);

    const differenceInTime = endDate - startDate;
    
    const differenceInDays = differenceInTime / (1000 * 3600 * 24);
    setNumberOfDays(differenceInDays + 1);
    };

    const navigate = useNavigate();

    return (
        <div className="2xl:flex 2xl:mt-[10vh] mt-[20px] gap-[100px]">
            <div key={item.id} className="2xl:ml-[13vw] ml-[25vw]">
                <img src={item.image} alt={item.title} className="rounded-xl w-[50vw] 2xl:w-[34vw]"/>
            </div>
            <div className="mt-[20px]">
                <h1 className="text-2xl 2xl:text-5xl font-semibold mb-[10px] text-center 2xl:w-[25vw]">{item.title}</h1>
                <p className="text-2xl 2xl:text-3xl mb-[20px] text-center">{item.price}</p>
                <p className="2xl:w-[26vw] mx-[4vw] mb-[20px] 2xl:mx-0 text-left 2xl:text-center">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. 
                Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
                <div className="mx-[6vw] flex justify-center">
                    <RangeCalendar aria-label="Date (No Selection)" isDateUnavailable={isDateUnavailable} onChange={calendarChangeHandler}/>
                </div>
                <div className="flex mt-[20px] gap-[20px] mx-[2vw] mb-[20px]">
                    <Button color={numberOfDays === 0 ? "danger" : "primary" } type="submit" className="w-[200px] text-[16px] py-[21px] font-semibold" onClick={()=> navigate(`checkout/${numberOfDays}`)} disabled={numberOfDays === 0}>
                        Rent Now
                    </Button>
                    <Button color="primary" type="submit" className="w-[200px] text-[16px] py-[21px] font-semibold bg-black" onClick={()=> navigate('reviews')}>
                        Reviews
                    </Button>
                </div>
                
            </div>
        </div>
        
    );
};

export default SpacePage;